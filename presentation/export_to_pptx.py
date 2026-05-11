from __future__ import annotations

import argparse
import html
import shutil
import sys
import tempfile
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile

from PIL import Image
from playwright.sync_api import sync_playwright

from render_index import render_index

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

SLIDE_W = 12192000
SLIDE_H = 6858000
VIEWPORT_W = 1600
VIEWPORT_H = 900
DEFAULT_ZOOM = 1.7


def xml_escape(value: str) -> str:
    return html.escape(value, quote=True)


def log(message: str) -> None:
    print(f"[presentation/export_to_pptx] {message}")


def standalone_slide_html(title: str, style_text: str, slide_html: str, index: int, count: int) -> str:
    return f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1.0"/>
<title>{html.escape(title)} - Slide {index} of {count}</title>
<style>
{style_text}
html,body{{width:100%;height:100%;overflow:hidden;background:#334155}}
.deck{{width:100vw;height:100vh}}
.slide{{position:relative;opacity:1!important;pointer-events:all!important;transform:none!important;transition:none!important;box-shadow:none!important}}
.nav,#prog{{display:none!important}}
</style>
</head>
<body>
<div class="deck">
{slide_html}
</div>
</body>
</html>
"""


def split_slides(html_path: Path, slides_dir: Path) -> list[Path]:
    log(f"Splitting {html_path.resolve()} into per-slide HTML under {slides_dir.resolve()}")
    slides_dir.mkdir(parents=True, exist_ok=True)
    old_slides = list(slides_dir.glob("slide-*.html"))
    if old_slides:
        log(f"Removing {len(old_slides)} existing generated slide files")
    for old_slide in old_slides:
        old_slide.unlink()

    file_url = html_path.resolve().as_uri()
    with sync_playwright() as p:
        log("Starting headless browser to read index deck")
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": VIEWPORT_W, "height": VIEWPORT_H})
        page.goto(file_url, wait_until="networkidle")
        data = page.evaluate(
            """() => ({
                title: document.title || 'HireWave Presentation',
                styleText: [...document.querySelectorAll('style')].map(s => s.textContent).join('\\n'),
                slides: [...document.querySelectorAll('.slide')].map((slide, index) => {
                    const clone = slide.cloneNode(true);
                    clone.classList.add('active');
                    clone.classList.remove('el');
                    clone.removeAttribute('style');
                    return clone.outerHTML;
                })
            })"""
        )
        browser.close()

    slides = data["slides"]
    if not slides:
        raise RuntimeError(f"No .slide elements found in {html_path}")
    log(f"Found {len(slides)} slides in index deck")

    paths: list[Path] = []
    for idx, slide_html in enumerate(slides, start=1):
        target = slides_dir / f"slide-{idx:02}.html"
        target.write_text(
            standalone_slide_html(data["title"], data["styleText"], slide_html, idx, len(slides)),
            encoding="utf-8",
        )
        paths.append(target)
        log(f"Wrote split slide {idx:02}: {target}")
    return paths


def normalize_image(image_path: Path) -> None:
    with Image.open(image_path) as image:
        rgb = image.convert("RGB")
        if rgb.size != (VIEWPORT_W, VIEWPORT_H):
            log(f"Normalizing {image_path.name} from {rgb.size} to {(VIEWPORT_W, VIEWPORT_H)}")
            rgb = rgb.resize((VIEWPORT_W, VIEWPORT_H), Image.Resampling.LANCZOS)
        rgb.save(image_path, "PNG")


def render_slide_files(slide_files: list[Path], image_dir: Path, zoom: float) -> list[Path]:
    css_width = round(VIEWPORT_W / zoom)
    css_height = round(VIEWPORT_H / zoom)
    log(
        f"Rendering {len(slide_files)} slide files at zoom {zoom:.2f} "
        f"(CSS viewport {css_width}x{css_height}, output {VIEWPORT_W}x{VIEWPORT_H})"
    )

    with sync_playwright() as p:
        log("Starting headless browser for slide screenshots")
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(
            viewport={"width": css_width, "height": css_height},
            device_scale_factor=zoom,
        )

        images: list[Path] = []
        for idx, slide_file in enumerate(slide_files, start=1):
            log(f"Rendering slide {idx:02}: {slide_file.name}")
            page.goto(slide_file.resolve().as_uri(), wait_until="networkidle")
            page.wait_for_timeout(80)
            target = image_dir / f"slide_{idx:02}.png"
            page.locator(".slide").screenshot(path=str(target))
            normalize_image(target)
            images.append(target)
            log(f"Wrote slide image {idx:02}: {target.name}")

        browser.close()
        log("Closed headless browser")
        return images


def render_slides(html_path: Path, image_dir: Path, slides_dir: Path, zoom: float) -> list[Path]:
    slide_files = split_slides(html_path, slides_dir)
    return render_slide_files(slide_files, image_dir, zoom)


def render_deck_slides(html_path: Path, image_dir: Path) -> list[Path]:
    log(f"Rendering active states directly from {html_path.resolve()}")
    file_url = html_path.resolve().as_uri()

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(
            viewport={"width": VIEWPORT_W, "height": VIEWPORT_H},
            device_scale_factor=1,
        )
        page.goto(file_url, wait_until="networkidle")
        page.add_style_tag(
            content="""
            .slide { transition: none !important; box-shadow: none !important; }
            .nav, #prog { display: none !important; }
            """
        )
        count = page.locator(".slide").count()
        if count == 0:
            raise RuntimeError(f"No .slide elements found in {html_path}")
        log(f"Found {count} slides in index deck")

        images: list[Path] = []
        for idx in range(count):
            log(f"Rendering deck slide state {idx + 1:02}")
            page.evaluate(
                """(idx) => {
                    const slides = [...document.querySelectorAll('.slide')];
                    slides.forEach((slide, i) => {
                        slide.classList.toggle('active', i === idx);
                        slide.classList.remove('el');
                        slide.removeAttribute('style');
                    });
                }""",
                idx,
            )
            page.wait_for_timeout(80)
            target = image_dir / f"slide_{idx + 1:02}.png"
            page.locator(".slide.active").screenshot(path=str(target))
            images.append(target)
            log(f"Wrote slide image {idx + 1:02}: {target.name}")

        browser.close()
        log("Closed headless browser")
        return images


def rels_xml(target: str, rel_type: str) -> str:
    return (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
        f'<Relationship Id="rId1" Type="{xml_escape(rel_type)}" Target="{xml_escape(target)}"/>'
        "</Relationships>"
    )


def content_types(slide_count: int) -> str:
    overrides = [
        '<Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>',
        '<Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/>',
        '<Override PartName="/ppt/slideLayouts/slideLayout1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"/>',
        '<Override PartName="/ppt/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>',
        '<Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>',
        '<Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>',
    ]
    overrides.extend(
        f'<Override PartName="/ppt/slides/slide{i}.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>'
        for i in range(1, slide_count + 1)
    )
    return (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
        '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
        '<Default Extension="xml" ContentType="application/xml"/>'
        '<Default Extension="png" ContentType="image/png"/>'
        + "".join(overrides)
        + "</Types>"
    )


def presentation_xml(slide_count: int) -> str:
    slide_ids = "".join(
        f'<p:sldId id="{255 + i}" r:id="rId{i}"/>' for i in range(1, slide_count + 1)
    )
    return f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:sldMasterIdLst><p:sldMasterId id="2147483648" r:id="rId{slide_count + 1}"/></p:sldMasterIdLst>
  <p:sldIdLst>{slide_ids}</p:sldIdLst>
  <p:sldSz cx="{SLIDE_W}" cy="{SLIDE_H}" type="wide"/>
  <p:notesSz cx="6858000" cy="9144000"/>
</p:presentation>'''


def presentation_rels(slide_count: int) -> str:
    rels = [
        f'<Relationship Id="rId{i}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide{i}.xml"/>'
        for i in range(1, slide_count + 1)
    ]
    rels.append(
        f'<Relationship Id="rId{slide_count + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="slideMasters/slideMaster1.xml"/>'
    )
    return (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
        + "".join(rels)
        + "</Relationships>"
    )


def slide_xml(index: int) -> str:
    name = f"Slide {index}"
    return f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld>
    <p:spTree>
      <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
      <p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr>
      <p:pic>
        <p:nvPicPr><p:cNvPr id="2" name="{xml_escape(name)}"/><p:cNvPicPr><a:picLocks noChangeAspect="1"/></p:cNvPicPr><p:nvPr/></p:nvPicPr>
        <p:blipFill><a:blip r:embed="rId1"/><a:stretch><a:fillRect/></a:stretch></p:blipFill>
        <p:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="{SLIDE_W}" cy="{SLIDE_H}"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></p:spPr>
      </p:pic>
    </p:spTree>
  </p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sld>'''


def slide_rels(index: int) -> str:
    return (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
        f'<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="../media/slide{index}.png"/>'
        "</Relationships>"
    )


def slide_master_xml() -> str:
    return f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldMaster xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld><p:bg><p:bgPr><a:solidFill><a:srgbClr val="FFFFFF"/></a:solidFill><a:effectLst/></p:bgPr></p:bg><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr></p:spTree></p:cSld>
  <p:clrMap bg1="lt1" tx1="dk1" bg2="lt2" tx2="dk2" accent1="accent1" accent2="accent2" accent3="accent3" accent4="accent4" accent5="accent5" accent6="accent6" hlink="hlink" folHlink="folHlink"/>
  <p:sldLayoutIdLst><p:sldLayoutId id="2147483649" r:id="rId1"/></p:sldLayoutIdLst>
  <p:txStyles><p:titleStyle/><p:bodyStyle/><p:otherStyle/></p:txStyles>
</p:sldMaster>'''


def slide_layout_xml() -> str:
    return '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldLayout xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" type="blank" preserve="1">
  <p:cSld name="Blank"><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr></p:spTree></p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sldLayout>'''


def theme_xml() -> str:
    return '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="HireWave Export">
  <a:themeElements>
    <a:clrScheme name="Office"><a:dk1><a:sysClr val="windowText" lastClr="000000"/></a:dk1><a:lt1><a:sysClr val="window" lastClr="FFFFFF"/></a:lt1><a:dk2><a:srgbClr val="1F497D"/></a:dk2><a:lt2><a:srgbClr val="EEECE1"/></a:lt2><a:accent1><a:srgbClr val="4F81BD"/></a:accent1><a:accent2><a:srgbClr val="C0504D"/></a:accent2><a:accent3><a:srgbClr val="9BBB59"/></a:accent3><a:accent4><a:srgbClr val="8064A2"/></a:accent4><a:accent5><a:srgbClr val="4BACC6"/></a:accent5><a:accent6><a:srgbClr val="F79646"/></a:accent6><a:hlink><a:srgbClr val="0000FF"/></a:hlink><a:folHlink><a:srgbClr val="800080"/></a:folHlink></a:clrScheme>
    <a:fontScheme name="Office"><a:majorFont><a:latin typeface="Calibri"/></a:majorFont><a:minorFont><a:latin typeface="Calibri"/></a:minorFont></a:fontScheme>
    <a:fmtScheme name="Office"><a:fillStyleLst/><a:lnStyleLst/><a:effectStyleLst/><a:bgFillStyleLst/></a:fmtScheme>
  </a:themeElements>
</a:theme>'''


def core_props() -> str:
    return '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <dc:title>HireWave Presentation</dc:title>
  <dc:creator>presentation/export_to_pptx.py</dc:creator>
</cp:coreProperties>'''


def app_props(slide_count: int) -> str:
    return f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
  <Application>Python</Application>
  <PresentationFormat>On-screen Show (16:9)</PresentationFormat>
  <Slides>{slide_count}</Slides>
</Properties>'''


def write_pptx(images: list[Path], output_path: Path) -> None:
    log(f"Writing PPTX with {len(images)} slides to {output_path.resolve()}")
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with ZipFile(output_path, "w", ZIP_DEFLATED) as pptx:
        pptx.writestr("[Content_Types].xml", content_types(len(images)))
        pptx.writestr(
            "_rels/.rels",
            '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
            '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
            '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>'
            '<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>'
            '<Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>'
            "</Relationships>",
        )
        pptx.writestr("docProps/core.xml", core_props())
        pptx.writestr("docProps/app.xml", app_props(len(images)))
        pptx.writestr("ppt/presentation.xml", presentation_xml(len(images)))
        pptx.writestr("ppt/_rels/presentation.xml.rels", presentation_rels(len(images)))
        pptx.writestr("ppt/slideMasters/slideMaster1.xml", slide_master_xml())
        pptx.writestr(
            "ppt/slideMasters/_rels/slideMaster1.xml.rels",
            rels_xml(
                "../slideLayouts/slideLayout1.xml",
                "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout",
            ),
        )
        pptx.writestr("ppt/slideLayouts/slideLayout1.xml", slide_layout_xml())
        pptx.writestr(
            "ppt/slideLayouts/_rels/slideLayout1.xml.rels",
            rels_xml(
                "../slideMasters/slideMaster1.xml",
                "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster",
            ),
        )
        pptx.writestr("ppt/theme/theme1.xml", theme_xml())

        for idx, image in enumerate(images, start=1):
            pptx.writestr(f"ppt/slides/slide{idx}.xml", slide_xml(idx))
            pptx.writestr(f"ppt/slides/_rels/slide{idx}.xml.rels", slide_rels(idx))
            pptx.write(image, f"ppt/media/slide{idx}.png")
            log(f"Embedded slide {idx:02} image: {image.name}")
    log(f"Finished writing {output_path.name}")


def export(
    html_path: Path,
    output_path: Path,
    slides_dir: Path,
    zoom: float = DEFAULT_ZOOM,
    keep_images: bool = False,
    source: str = "slides",
) -> None:
    temp_dir = Path(tempfile.mkdtemp(prefix="hirewave_pptx_"))
    log(f"Using temporary render directory {temp_dir}")
    try:
        if source == "slides":
            slide_files = sorted(slides_dir.glob("slide-*.html"))
            if slide_files:
                log(f"Using slides directory as source of truth: {slides_dir.resolve()}")
                render_index(slides_dir, html_path)
                images = render_slide_files(slide_files, temp_dir, zoom)
            else:
                log("No per-slide HTML files found; falling back to splitting index.html")
                images = render_slides(html_path, temp_dir, slides_dir, zoom)
        else:
            log("Using index.html as source of truth and regenerating per-slide HTML")
            images = render_slides(html_path, temp_dir, slides_dir, zoom)
        write_pptx(images, output_path)
        if keep_images:
            image_output = output_path.with_suffix("")
            image_output.mkdir(parents=True, exist_ok=True)
            log(f"Keeping rendered PNG files in {image_output.resolve()}")
            for image in images:
                shutil.copy2(image, image_output / image.name)
                log(f"Copied {image.name} to {image_output}")
    finally:
        shutil.rmtree(temp_dir, ignore_errors=True)
        log(f"Removed temporary render directory {temp_dir}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Render presentation/index.html to PPTX.")
    parser.add_argument(
        "html",
        nargs="?",
        default=Path(__file__).with_name("index.html"),
        type=Path,
        help="Path to the HTML slide deck.",
    )
    parser.add_argument(
        "-o",
        "--output",
        default=Path(__file__).with_name("hirewave_presentation.pptx"),
        type=Path,
        help="Output .pptx path.",
    )
    parser.add_argument(
        "--keep-images",
        action="store_true",
        help="Also save the rendered slide PNG files beside the PPTX.",
    )
    parser.add_argument(
        "--slides-dir",
        default=Path(__file__).with_name("slides"),
        type=Path,
        help="Directory where standalone per-slide HTML files are written.",
    )
    parser.add_argument(
        "--zoom",
        default=DEFAULT_ZOOM,
        type=float,
        help="Responsive browser zoom used for rendering each slide into the PPTX.",
    )
    parser.add_argument(
        "--source",
        choices=["slides", "index"],
        default="slides",
        help="Use per-slide HTML as source of truth, or rebuild slides from index.html first.",
    )
    args = parser.parse_args()

    log("Starting PPTX export")
    export(args.html, args.output, args.slides_dir, args.zoom, args.keep_images, args.source)
    if args.source == "slides":
        log(f"Rendered {args.html} from per-slide HTML in {args.slides_dir}")
    else:
        log(f"Wrote per-slide HTML to {args.slides_dir}")
    log("Finished PPTX export")


if __name__ == "__main__":
    main()
