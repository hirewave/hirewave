from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

from playwright.sync_api import sync_playwright

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

VIEWPORT = {"width": 1600, "height": 900}

STANDALONE_CSS_RE = re.compile(
    r"\nhtml,body\{width:100%;height:100%;overflow:hidden;background:#334155\}"
    r"\n\.deck\{width:100vw;height:100vh\}"
    r"\n\.slide\{position:relative;opacity:1!important;pointer-events:all!important;transform:none!important;transition:none!important;box-shadow:none!important\}"
    r"\n\.nav,#prog\{display:none!important\}",
)

NAV_AND_SCRIPT = """</div><!-- /deck -->

<!-- Navigation controls: generated dots, previous/next buttons, and live slide count. -->
<nav class="nav">
  <button class="nb" id="bp" onclick="go(-1)">‹</button>
  <div class="dots" id="dots"></div>
  <span class="nc" id="nc">1 / {slide_count}</span>
  <button class="nb" id="bn" onclick="go(1)">›</button>
</nav>

<script>
const S=[...document.querySelectorAll('.slide')];
const N=S.length,P=document.getElementById('prog');
let c=0,lk=false;

const D=document.getElementById('dots');
S.forEach((_,i)=>{
  const d=document.createElement('div');
  d.className='dot'+(i===0?' on':'');
  d.onclick=()=>go(i-c);
  D.appendChild(d);
});

function sync(){
  document.getElementById('nc').textContent=`${c+1} / ${N}`;
  document.getElementById('bp').disabled=c===0;
  document.getElementById('bn').disabled=c===N-1;
  D.querySelectorAll('.dot').forEach((d,i)=>d.classList.toggle('on',i===c));
  P.style.width=`${(c+1)/N*100}%`;
}

function go(d){
  if(lk)return;
  const nx=c+d;if(nx<0||nx>=N)return;
  lk=true;
  S[c].classList.remove('active');S[c].classList.add('el');
  const s=S[nx];
  s.style.transform=d>0?'translateX(40px)':'translateX(-40px)';
  s.style.opacity='0';s.style.pointerEvents='all';
  requestAnimationFrame(()=>requestAnimationFrame(()=>{
    s.style.transition='opacity .3s ease,transform .3s ease';
    s.style.transform='translateX(0)';s.style.opacity='1';
  }));
  setTimeout(()=>{
    S[c].classList.remove('el');s.classList.add('active');s.removeAttribute('style');
    c=nx;sync();lk=false;
  },310);
}

document.addEventListener('keydown',e=>{
  if(e.key==='ArrowRight'||e.key===' '){e.preventDefault();go(1)}
  if(e.key==='ArrowLeft'){e.preventDefault();go(-1)}
  if(e.key==='Home')go(-c);if(e.key==='End')go(N-1-c);
});
let tx=null;
document.addEventListener('touchstart',e=>{tx=e.touches[0].clientX});
document.addEventListener('touchend',e=>{
  if(tx===null)return;const dx=e.changedTouches[0].clientX-tx;
  if(Math.abs(dx)>50)go(dx<0?1:-1);tx=null;
});
sync();
</script>
</body>
</html>

<!-- Generated from presentation/slides/*.html by render_index.py. -->
"""


def clean_style(style_text: str) -> str:
    return STANDALONE_CSS_RE.sub("", style_text).strip()


def log(message: str) -> None:
    print(f"[presentation/render_index] {message}")


def read_slide_sources(slides_dir: Path) -> tuple[str, str, list[tuple[str, str]]]:
    log(f"Reading slide HTML files from {slides_dir.resolve()}")
    slide_files = sorted(slides_dir.glob("slide-*.html"))
    if not slide_files:
        raise RuntimeError(f"No slide-*.html files found in {slides_dir}")
    log(f"Found {len(slide_files)} slide files")

    with sync_playwright() as p:
        log("Starting headless browser to parse slide DOM")
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport=VIEWPORT)

        title = "HireWave - Final Presentation"
        style_text = ""
        slides: list[tuple[str, str]] = []

        for idx, slide_file in enumerate(slide_files, start=1):
            log(f"Loading slide {idx:02}: {slide_file.name}")
            page.goto(slide_file.resolve().as_uri(), wait_until="networkidle")
            if idx == 1:
                title = page.title().split(" - Slide ", 1)[0] or title
                log(f"Using deck title: {title}")
                style_text = page.evaluate(
                    "() => [...document.querySelectorAll('style')].map(s => s.textContent).join('\\n')"
                )
                log(f"Captured shared CSS: {len(style_text)} characters")

            slide_html = page.evaluate(
                """(idx) => {
                    const slide = document.querySelector('.slide');
                    if (!slide) return '';
                    const clone = slide.cloneNode(true);
                    clone.removeAttribute('style');
                    clone.classList.remove('el');
                    clone.classList.toggle('active', idx === 1);
                    return clone.outerHTML;
                }""",
                idx,
            )
            if not slide_html:
                raise RuntimeError(f"No .slide element found in {slide_file}")
            slide_html = slide_html.replace("../images/", "images/")

            heading = page.evaluate(
                """() => {
                    const title = document.querySelector('.sh-title,.ts-title,.section-title');
                    return title ? title.textContent.trim().replace(/\\s+/g, ' ') : '';
                }"""
            )
            slides.append((heading or f"Slide {idx}", slide_html))
            log(f"Captured slide {idx:02} heading: {heading or f'Slide {idx}'}")

        browser.close()
        log("Closed headless browser")

    return title, clean_style(style_text), slides


def build_index(title: str, style_text: str, slides: list[tuple[str, str]]) -> str:
    log(f"Building index HTML with {len(slides)} slides")
    slide_sections: list[str] = []
    for idx, (heading, slide_html) in enumerate(slides, start=1):
        slide_sections.append(
            f"""<!-- ============================================================
  Slide {idx:02}: {heading}
  Source: slides/slide-{idx:02}.html
============================================================ -->
{slide_html}"""
        )

    return f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1.0"/>
<title>{title}</title>
<style>
{style_text}
</style>
</head>
<body>
<!-- Progress bar is controlled by the navigation script at the bottom. -->
<div id="prog"></div>

<!-- Deck container: every .slide below comes from presentation/slides/slide-XX.html. -->
<div class="deck" id="deck">

{chr(10).join(slide_sections)}

{NAV_AND_SCRIPT.replace("{slide_count}", str(len(slides)))}
"""


def render_index(slides_dir: Path, output_path: Path) -> None:
    title, style_text, slides = read_slide_sources(slides_dir)
    output_path.write_text(build_index(title, style_text, slides), encoding="utf-8", newline="\n")
    log(f"Wrote {output_path.resolve()} ({len(slides)} slides)")


def main() -> None:
    parser = argparse.ArgumentParser(description="Rebuild presentation/index.html from per-slide HTML files.")
    parser.add_argument(
        "--slides-dir",
        default=Path(__file__).with_name("slides"),
        type=Path,
        help="Directory containing slide-*.html files.",
    )
    parser.add_argument(
        "-o",
        "--output",
        default=Path(__file__).with_name("index.html"),
        type=Path,
        help="Output index.html path.",
    )
    args = parser.parse_args()
    log("Starting index render")
    render_index(args.slides_dir, args.output)
    log("Finished index render")


if __name__ == "__main__":
    main()
