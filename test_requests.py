#!/usr/bin/env python3
"""
HireWave API end-to-end smoke test.
Covers every endpoint in requests.http with real payloads.
Run:  python3 test_requests.py
"""
import sys
import requests
from colorama import init, Fore, Style

init(autoreset=True)

BASE = "http://localhost:8080"
PASS = f"{Fore.GREEN}✓ PASS{Style.RESET_ALL}"
FAIL = f"{Fore.RED}✗ FAIL{Style.RESET_ALL}"

results = []


def check(label, method, url, expected_status, **kwargs):
    import json as _json
    full_url = BASE + url
    short = f"{method.upper():<6} {url}"
    try:
        resp = getattr(requests, method)(full_url, timeout=5, **kwargs)
        ok = resp.status_code == expected_status
        tag = PASS if ok else FAIL
        status_color = Fore.GREEN if ok else Fore.RED
        print(f"  {tag}  {short:<60} {status_color}{resp.status_code}{Style.RESET_ALL}")
        if not ok:
            payload = kwargs.get("json")
            if payload is not None:
                print(f"       {Fore.YELLOW}Payload : {_json.dumps(payload, ensure_ascii=False)}{Style.RESET_ALL}")
            try:
                body = resp.json()
                pretty = _json.dumps(body, indent=6, ensure_ascii=False)
            except Exception:
                pretty = resp.text[:500]
            print(f"       {Fore.RED}Response: {pretty}{Style.RESET_ALL}")
        results.append(ok)
        return resp
    except Exception as exc:
        print(f"  {FAIL}  {short:<60} {Fore.RED}ERROR: {exc}{Style.RESET_ALL}")
        results.append(False)
        return None


def section(title):
    print(f"\n{Fore.CYAN}{Style.BRIGHT}{'─'*70}")
    print(f"  {title}")
    print(f"{'─'*70}{Style.RESET_ALL}")


def cleanup():
    """Delete all test entities so every run starts clean."""
    print(f"\n{Fore.MAGENTA}{Style.BRIGHT}  Cleaning up previous test data...{Style.RESET_ALL}")

    # Users by email
    for email in ["prisoner24601@france.fr"]:
        r = requests.get(BASE + f"/api/users/by-email?email={email}", timeout=5)
        if r.ok:
            uid = r.json().get("id")
            requests.delete(BASE + f"/api/users/{uid}", timeout=5)

    # Freelancers
    r = requests.get(BASE + "/api/freelancers", timeout=5)
    for f in (r.json() if r.ok else []):
        if f.get("email") in {"terry@templeos.org", "ada@lovelace.dev"}:
            requests.delete(BASE + f"/api/freelancers/{f['id']}", timeout=5)

    # Clients (email may have been updated to projects@broski.io on a previous run)
    r = requests.get(BASE + "/api/clients", timeout=5)
    scenario_client_emails = {
        "hiring@broski.io", "projects@broski.io",
        "techcorp@example.com", "startupx@example.com",
        "agency@example.com", "reputation@example.com", "lifecycle@example.com",
        "fl-s7-counterstorm@example.com",
    }
    scenario_freelancer_prefixes = (
        "dev", "ada@", "terry@",
        "fl-s",   # scenario freelancers
    )
    for c in (r.json() if r.ok else []):
        if c.get("email") in scenario_client_emails:
            requests.delete(BASE + f"/api/clients/{c['id']}", timeout=5)

    # Scenario freelancers — wipe all whose email starts with "fl-s"
    r = requests.get(BASE + "/api/freelancers", timeout=5)
    for f in (r.json() if r.ok else []):
        e = f.get("email", "")
        if e in {"terry@templeos.org", "ada@lovelace.dev"} or e.startswith("fl-s"):
            requests.delete(BASE + f"/api/freelancers/{f['id']}", timeout=5)

    print(f"  {Fore.MAGENTA}  Done.{Style.RESET_ALL}")


cleanup()

# ─── USERS ────────────────────────────────────────────────────────────────────
section("USER CONTROLLER")

r = check("GET all users (empty or existing)", "get", "/api/users", 200)

r = check("POST create user", "post", "/api/users",
          201, json={"name": "Jean Valjean", "email": "prisoner24601@france.fr"})
user_id = r.json()["id"] if r and r.ok else None

if user_id:
    check("GET user by ID", "get", f"/api/users/{user_id}", 200)
    check("GET user by email", "get", "/api/users/by-email?email=prisoner24601@france.fr", 200)
    check("PUT update user name", "put", f"/api/users/{user_id}",
          200, json={"name": "Monsieur Madeleine"})
    check("PATCH change name", "patch", f"/api/users/{user_id}/name",
          200, json={"name": "Monsieur le Maire"})
else:
    # User already exists – fetch it
    r2 = requests.get(BASE + "/api/users/by-email?email=prisoner24601@france.fr")
    if r2.ok:
        user_id = r2.json()["id"]
        print(f"  {Fore.YELLOW}  (user already exists, id={user_id}){Style.RESET_ALL}")
        check("GET user by ID", "get", f"/api/users/{user_id}", 200)

# ─── TODOS ────────────────────────────────────────────────────────────────────
section("TODO CONTROLLER")

r = check("POST create todo", "post", "/api/todos",
          201, json={"description": "Rescue Cosette from the Thénardiers",
                     "assigneeEmail": "prisoner24601@france.fr"})
todo_id = r.json()["id"] if r and r.ok else None

if todo_id:
    check("GET todos by email", "get", "/api/todos?assigneeEmail=prisoner24601@france.fr", 200)
    check("GET todo by ID", "get", f"/api/todos/{todo_id}", 200)
    check("PATCH mark as done", "patch", f"/api/todos/{todo_id}/done",
          200, json=True, headers={"Content-Type": "application/json"})
    check("PATCH edit description", "patch", f"/api/todos/{todo_id}/description",
          200, json={"description": "Escape from Inspector Javert"})
    check("PATCH assign to frodo", "patch", f"/api/todos/{todo_id}/assignee",
          200, json={"newAssigneeEmail": "frodo@theshire.me"})

# ─── CLIENTS ──────────────────────────────────────────────────────────────────
section("CLIENT CONTROLLER")

check("GET all clients", "get", "/api/clients", 200)

r = check("POST create client", "post", "/api/clients",
          201, json={"name": "Broski Corporation", "email": "hiring@broski.io"})
client_id = r.json()["id"] if r and r.ok else None

if not client_id:
    r2 = requests.get(BASE + "/api/clients")
    for c in (r2.json() if r2.ok else []):
        if c.get("email") == "hiring@broski.io":
            client_id = c["id"]
            print(f"  {Fore.YELLOW}  (client already exists, id={client_id}){Style.RESET_ALL}")
            break

if client_id:
    check("GET client by ID", "get", f"/api/clients/{client_id}", 200)
    check("PUT update client", "put", f"/api/clients/{client_id}",
          200, json={"name": "Broski Corporation Ltd", "email": "projects@broski.io"})
    # update email changed, use new one going forward
    client_email = "projects@broski.io"

# ─── FREELANCERS ──────────────────────────────────────────────────────────────
section("FREELANCER CONTROLLER")

check("GET all freelancers", "get", "/api/freelancers", 200)

r = check("POST create freelancer", "post", "/api/freelancers",
          201, json={"name": "Terry Davis", "email": "terry@templeos.org",
                     "skills": ["Java", "Spring Boot", "MongoDB"], "hourlyRate": 100.0})
freelancer_id = r.json()["id"] if r and r.ok else None

if not freelancer_id:
    r2 = requests.get(BASE + "/api/freelancers")
    for f in (r2.json() if r2.ok else []):
        if f.get("email") == "terry@templeos.org":
            freelancer_id = f["id"]
            print(f"  {Fore.YELLOW}  (freelancer already exists, id={freelancer_id}){Style.RESET_ALL}")
            break

if freelancer_id:
    check("GET freelancer by ID", "get", f"/api/freelancers/{freelancer_id}", 200)
    check("PUT update freelancer", "put", f"/api/freelancers/{freelancer_id}",
          200, json={"name": "Terry A. Davis", "email": "terry@templeos.org",
                     "skills": ["Java", "Spring Boot", "MongoDB", "HolyC"],
                     "hourlyRate": 110.0})
    check("POST rate freelancer", "post", f"/api/freelancers/{freelancer_id}/ratings",
          200, json={"rating": 5})

# ─── PROJECTS ─────────────────────────────────────────────────────────────────
section("PROJECT CONTROLLER")

check("GET all projects", "get", "/api/projects", 200)

project_id = None
if client_id:
    r = check("POST create project", "post", "/api/projects",
              201, json={"title": "Build a REST API",
                         "description": "A scalable REST API in Java + Spring Boot",
                         "clientId": client_id,
                         "requiredSkills": ["Java", "Spring Boot", "MongoDB"],
                         "budget": 5000.0})
    project_id = r.json()["id"] if r and r.ok else None

    if not project_id:
        r2 = requests.get(BASE + "/api/projects")
        for p in (r2.json() if r2.ok else []):
            if p.get("clientId") == client_id and p.get("status") == "OPEN":
                project_id = p["id"]
                print(f"  {Fore.YELLOW}  (project already exists, id={project_id}){Style.RESET_ALL}")
                break

if project_id:
    check("GET project by ID", "get", f"/api/projects/{project_id}", 200)
    check("GET projects by client", "get", f"/api/projects/by-client/{client_id}", 200)
    check("PUT update project", "put", f"/api/projects/{project_id}",
          200, json={"title": "Build a Scalable REST API",
                     "description": "Updated with full test coverage",
                     "requiredSkills": ["Java", "Spring Boot", "MongoDB", "Docker"],
                     "budget": 6000.0})

# ─── BIDS ─────────────────────────────────────────────────────────────────────
section("BID CONTROLLER")

bid_id = None
bid2_id = None

if project_id and freelancer_id:
    check("GET bids for project (empty)", "get", f"/api/bids?projectId={project_id}", 200)

    r = check("POST submit bid", "post", "/api/bids",
              201, json={"projectId": project_id,
                         "freelancerId": freelancer_id,
                         "amount": 4500.0,
                         "message": "5 years Java+Spring, deliver on time."})
    bid_id = r.json()["id"] if r and r.ok else None

    if not bid_id:
        r2 = requests.get(BASE + f"/api/bids?projectId={project_id}")
        bids = r2.json() if r2.ok else []
        if bids:
            bid_id = bids[0]["id"]
            print(f"  {Fore.YELLOW}  (bid already exists, id={bid_id}){Style.RESET_ALL}")

    # Create a second freelancer and second bid so we can test the cascade reject
    r2 = check("POST create 2nd freelancer", "post", "/api/freelancers",
               201, json={"name": "Ada Lovelace", "email": "ada@lovelace.dev",
                          "skills": ["Java", "MongoDB"], "hourlyRate": 90.0})
    fl2_id = r2.json()["id"] if r2 and r2.ok else None
    if not fl2_id:
        r3 = requests.get(BASE + "/api/freelancers")
        for f in (r3.json() if r3.ok else []):
            if f.get("email") == "ada@lovelace.dev":
                fl2_id = f["id"]
                break

    if fl2_id:
        r3 = check("POST submit 2nd bid", "post", "/api/bids",
                   201, json={"projectId": project_id,
                              "freelancerId": fl2_id,
                              "amount": 4200.0,
                              "message": "Well-versed in Java and Mongo."})
        bid2_id = r3.json()["id"] if r3 and r3.ok else None

    if bid_id:
        check("GET bid by ID", "get", f"/api/bids/{bid_id}", 200)
        check("GET bids for project (has bids)", "get", f"/api/bids?projectId={project_id}", 200)

    # Test duplicate bid rejection
    check("POST duplicate bid → 400", "post", "/api/bids",
          400, json={"projectId": project_id,
                     "freelancerId": freelancer_id,
                     "amount": 3000.0,
                     "message": "Trying to bid again."})

    # Accept bid #1 — should cascade reject bid #2 and move project to IN_PROGRESS
    if bid_id:
        check("PATCH accept bid (cascade)", "patch", f"/api/bids/{bid_id}/accept", 200)

        if bid2_id:
            r_b2 = requests.get(BASE + f"/api/bids/{bid2_id}")
            if r_b2.ok and r_b2.json().get("status") == "REJECTED":
                print(f"  {PASS}  {'CASCADE: 2nd bid auto-rejected':<60} {Fore.GREEN}REJECTED{Style.RESET_ALL}")
                results.append(True)
            else:
                print(f"  {FAIL}  {'CASCADE: 2nd bid auto-rejected':<60} {Fore.RED}status={r_b2.json().get('status')}{Style.RESET_ALL}")
                results.append(False)

        r_proj = requests.get(BASE + f"/api/projects/{project_id}")
        if r_proj.ok and r_proj.json().get("status") == "IN_PROGRESS":
            print(f"  {PASS}  {'CASCADE: project moved to IN_PROGRESS':<60} {Fore.GREEN}IN_PROGRESS{Style.RESET_ALL}")
            results.append(True)
        else:
            print(f"  {FAIL}  {'CASCADE: project moved to IN_PROGRESS':<60} {Fore.RED}status={r_proj.json().get('status')}{Style.RESET_ALL}")
            results.append(False)

    # Reject-already-accepted should fail
    if bid_id:
        check("PATCH reject accepted bid → 400", "patch", f"/api/bids/{bid_id}/reject", 400)

    # Complete the project (IN_PROGRESS → COMPLETED)
    if project_id:
        check("PATCH complete project", "patch", f"/api/projects/{project_id}/complete", 200)

# ─── CANCEL FLOW (fresh project) ─────────────────────────────────────────────
section("PROJECT CANCEL FLOW")
if client_id:
    r = check("POST create project to cancel", "post", "/api/projects",
              201, json={"title": "Doomed Project",
                         "description": "Will be cancelled immediately",
                         "clientId": client_id,
                         "requiredSkills": ["Python"],
                         "budget": 100.0})
    cancel_proj_id = r.json()["id"] if r and r.ok else None
    if cancel_proj_id:
        check("PATCH cancel project", "patch", f"/api/projects/{cancel_proj_id}/cancel", 200)
        check("PATCH cancel already-cancelled → 400", "patch", f"/api/projects/{cancel_proj_id}/cancel", 400)

# ─── VALIDATION ERRORS ────────────────────────────────────────────────────────
section("VALIDATION / ERROR CASES")

check("POST freelancer missing skills → 400", "post", "/api/freelancers",
      400, json={"name": "No Skills", "email": "noskills@x.com",
                 "skills": [], "hourlyRate": 50.0})
check("POST client missing email → 400", "post", "/api/clients",
      400, json={"name": "No Email"})
check("GET non-existent project → 404", "get", "/api/projects/nonexistentid000", 404)
check("GET non-existent bid → 404", "get", "/api/bids/nonexistentid000", 404)
check("GET non-existent freelancer → 404", "get", "/api/freelancers/nonexistentid000", 404)

# ══════════════════════════════════════════════════════════════════════════════
# SCENARIO TESTS
# ══════════════════════════════════════════════════════════════════════════════

def scenario(title, description):
    print(f"\n{Fore.YELLOW}{Style.BRIGHT}{'═'*70}")
    print(f"  SCENARIO: {title}")
    print(f"  {description}")
    print(f"{'═'*70}{Style.RESET_ALL}")

def assert_field(label, actual, expected):
    ok = actual == expected
    tag = PASS if ok else FAIL
    color = Fore.GREEN if ok else Fore.RED
    print(f"  {tag}  {label:<60} {color}{actual!r}{Style.RESET_ALL}")
    results.append(ok)


# ─── SCENARIO 1 ───────────────────────────────────────────────────────────────
scenario(
    "Competitive Bidding Race — 20 freelancers, only 10 qualify by skill",
    "TechCorp posts a Java/Spring project. 20 freelancers sign up: 10 have Java,\n"
    "  10 do not. All 20 attempt to bid. Exactly 10 succeed, 10 are rejected.\n"
    "  The lowest bid wins. All other 9 valid bids are cascade-rejected on accept."
)

r = check("Create client TechCorp", "post", "/api/clients",
          201, json={"name": "TechCorp", "email": "techcorp@example.com"})
s1_client_id = r.json()["id"] if r and r.ok else None

r = check("Create Java project", "post", "/api/projects",
          201, json={"title": "Enterprise Portal",
                     "description": "Spring Boot monolith migration",
                     "clientId": s1_client_id,
                     "requiredSkills": ["Java", "Spring Boot"],
                     "budget": 15000.0})
s1_proj_id = r.json()["id"] if r and r.ok else None

s1_fl_ids   = []   # freelancers with matching skill → can bid
s1_fl_no_ids = []  # freelancers with wrong skill   → cannot bid

for i in range(1, 21):
    has_skill = i <= 10
    skills    = ["Java", "Spring Boot"] if has_skill else ["PHP", "Ruby"]
    rate      = 50.0 + i * 5          # rates from 55 to 150
    email     = f"fl-s1-{'yes' if has_skill else 'no'}-{i:02d}@example.com"
    r = requests.post(BASE + "/api/freelancers", timeout=5,
                      json={"name": f"Dev {i:02d}", "email": email,
                            "skills": skills, "hourlyRate": rate})
    fid = r.json()["id"] if r.ok else None
    if has_skill:
        s1_fl_ids.append((fid, rate, email))
    else:
        s1_fl_no_ids.append((fid, rate, email))

print(f"  {Fore.WHITE}  Created 10 Java-skilled + 10 non-skilled freelancers{Style.RESET_ALL}")

accepted_bids = 0
rejected_bids = 0
bid_amounts   = []
all_bid_ids   = []

if s1_proj_id:
    for fid, rate, email in s1_fl_ids:
        amount = rate * 100
        r = requests.post(BASE + "/api/bids", timeout=5,
                          json={"projectId": s1_proj_id, "freelancerId": fid,
                                "amount": amount, "message": f"I know Java, rate=${rate}/hr"})
        if r.status_code == 201:
            accepted_bids += 1
            bid_amounts.append((amount, r.json()["id"], fid))
            all_bid_ids.append(r.json()["id"])
        else:
            rejected_bids += 1

    for fid, rate, email in s1_fl_no_ids:
        r = requests.post(BASE + "/api/bids", timeout=5,
                          json={"projectId": s1_proj_id, "freelancerId": fid,
                                "amount": rate * 80, "message": "Please hire me"})
        if r.status_code == 400:
            rejected_bids += 1
        else:
            accepted_bids += 1  # should not happen

assert_field("Skilled freelancers bid accepted (expect 10)",  accepted_bids, 10)
assert_field("Unskilled bids rejected by API (expect 10)",    rejected_bids, 10)

# Accept the cheapest bid
if bid_amounts:
    bid_amounts.sort()
    cheapest_amount, winning_bid_id, winner_fid = bid_amounts[0]
    r = check("Accept cheapest bid", "patch", f"/api/bids/{winning_bid_id}/accept", 200)

    # All others must be REJECTED
    other_ids   = [b for b in all_bid_ids if b != winning_bid_id]
    still_pending = sum(
        1 for bid_id in other_ids
        if requests.get(BASE + f"/api/bids/{bid_id}", timeout=5).json().get("status") != "REJECTED"
    )
    assert_field("All 9 other bids cascade-rejected (expect 0 still pending)", still_pending, 0)

    r_proj = requests.get(BASE + f"/api/projects/{s1_proj_id}", timeout=5).json()
    assert_field("Project status is IN_PROGRESS", r_proj.get("status"), "IN_PROGRESS")
    assert_field("Awarded freelancer matches winner", r_proj.get("awardedFreelancerId"), winner_fid)


# ─── SCENARIO 2 ───────────────────────────────────────────────────────────────
scenario(
    "Freelancer Capacity Wall — blocked after 3 concurrent active projects",
    "A single freelancer wins 3 different projects (all IN_PROGRESS). When they\n"
    "  try to bid on a 4th OPEN project, the API blocks them with 400."
)

r = check("Create client StartupX", "post", "/api/clients",
          201, json={"name": "StartupX", "email": "startupx@example.com"})
s2_client_id = r.json()["id"] if r and r.ok else None

r = requests.post(BASE + "/api/freelancers", timeout=5,
                  json={"name": "Max Load", "email": "fl-s2-max@example.com",
                        "skills": ["Python", "Django"], "hourlyRate": 70.0})
s2_fl_id = r.json()["id"] if r.ok else None
print(f"  {Fore.WHITE}  Freelancer 'Max Load' created, will fill up 3 projects{Style.RESET_ALL}")

if s2_client_id and s2_fl_id:
    for i in range(1, 5):   # create 4 projects; win the first 3, blocked on 4th
        rp = requests.post(BASE + "/api/projects", timeout=5,
                           json={"title": f"StartupX Job {i}",
                                 "description": f"Django project #{i}",
                                 "clientId": s2_client_id,
                                 "requiredSkills": ["Python", "Django"],
                                 "budget": 2000.0 * i})
        pid = rp.json()["id"] if rp.ok else None
        if not pid:
            continue

        rb = requests.post(BASE + "/api/bids", timeout=5,
                           json={"projectId": pid, "freelancerId": s2_fl_id,
                                 "amount": 1500.0 * i, "message": "Can do!"})

        if i < 4:           # projects 1-3: accept bid → IN_PROGRESS
            bid_id_here = rb.json()["id"] if rb.ok else None
            if bid_id_here:
                requests.patch(BASE + f"/api/bids/{bid_id_here}/accept", timeout=5)
            # do NOT complete — keep them IN_PROGRESS to saturate the limit
        else:               # project 4 bid: should be blocked
            assert_field(
                "4th bid blocked — freelancer at max capacity (expect 400)",
                rb.status_code, 400
            )
            detail = rb.json().get("error", "")
            ok = "3 concurrent" in detail or "concurrent" in detail
            tag = PASS if ok else FAIL
            print(f"  {tag}  {'Error message mentions concurrent limit':<60} {Fore.GREEN if ok else Fore.RED}{detail[:60]!r}{Style.RESET_ALL}")
            results.append(ok)


# ─── SCENARIO 3 ───────────────────────────────────────────────────────────────
scenario(
    "Skill-Mismatch Filter — diverse project with rare skills",
    "Agency Ltd posts a niche ML project requiring 'TensorFlow' and 'Rust'.\n"
    "  5 freelancers apply: 2 have TensorFlow, 1 has Rust, 2 have neither.\n"
    "  Only the 3 with at least one match should be allowed. The remaining 2 are blocked."
)

r = check("Create client Agency Ltd", "post", "/api/clients",
          201, json={"name": "Agency Ltd", "email": "agency@example.com"})
s3_client_id = r.json()["id"] if r and r.ok else None

r = check("Create ML/Rust project", "post", "/api/projects",
          201, json={"title": "AI Model Training Platform",
                     "description": "TensorFlow pipeline with a Rust backend",
                     "clientId": s3_client_id,
                     "requiredSkills": ["TensorFlow", "Rust"],
                     "budget": 12000.0})
s3_proj_id = r.json()["id"] if r and r.ok else None

s3_devs = [
    ("fl-s3-a", "Alice TF",   ["TensorFlow", "Python"],   True),
    ("fl-s3-b", "Bob Rust",   ["Rust", "C++"],             True),
    ("fl-s3-c", "Carol Both", ["TensorFlow", "Rust"],      True),
    ("fl-s3-d", "Dan PHP",    ["PHP", "Laravel"],          False),
    ("fl-s3-e", "Eva Ruby",   ["Ruby", "Rails"],           False),
]

s3_qualifier_bids = []

if s3_proj_id:
    for slug, name, skills, should_qualify in s3_devs:
        email = f"{slug}@example.com"
        rfl = requests.post(BASE + "/api/freelancers", timeout=5,
                            json={"name": name, "email": email,
                                  "skills": skills, "hourlyRate": 80.0})
        fid = rfl.json()["id"] if rfl.ok else None
        if not fid:
            continue

        rb = requests.post(BASE + "/api/bids", timeout=5,
                           json={"projectId": s3_proj_id, "freelancerId": fid,
                                 "amount": 9000.0, "message": f"{name} applying"})
        got_in = rb.status_code == 201
        label  = f"{name} ({'qualified' if should_qualify else 'no match'}) bid {'accepted' if should_qualify else 'rejected'}"
        assert_field(label, got_in, should_qualify)
        if got_in:
            s3_qualifier_bids.append(rb.json()["id"])

assert_field("Exactly 3 qualifying bids placed", len(s3_qualifier_bids), 3)


# ─── SCENARIO 4 ───────────────────────────────────────────────────────────────
scenario(
    "Client Reputation Score — tracks completed vs cancelled projects",
    "Reputation Inc. runs 5 projects: 3 completed, 2 cancelled.\n"
    "  Expected reputation = 3/(3+2) = 0.60."
)

r = check("Create Reputation Inc. client", "post", "/api/clients",
          201, json={"name": "Reputation Inc.", "email": "reputation@example.com"})
s4_client_id = r.json()["id"] if r and r.ok else None

# Need a helper freelancer with matching skills
r = requests.post(BASE + "/api/freelancers", timeout=5,
                  json={"name": "Workhorse", "email": "fl-s4-worker@example.com",
                        "skills": ["Go", "Docker"], "hourlyRate": 60.0})
s4_fl_id = r.json()["id"] if r.ok else None

if s4_client_id and s4_fl_id:
    # 3 COMPLETED projects
    for i in range(1, 4):
        rp = requests.post(BASE + "/api/projects", timeout=5,
                           json={"title": f"Completed Project {i}",
                                 "description": "A Go micro-service",
                                 "clientId": s4_client_id,
                                 "requiredSkills": ["Go", "Docker"],
                                 "budget": 3000.0})
        pid = rp.json()["id"] if rp.ok else None
        if not pid:
            continue
        rb = requests.post(BASE + "/api/bids", timeout=5,
                           json={"projectId": pid, "freelancerId": s4_fl_id,
                                 "amount": 2500.0, "message": "On it"})
        if rb.ok:
            requests.patch(BASE + f"/api/bids/{rb.json()['id']}/accept", timeout=5)
            requests.patch(BASE + f"/api/projects/{pid}/complete", timeout=5)

    # 2 CANCELLED projects
    for i in range(1, 3):
        rp = requests.post(BASE + "/api/projects", timeout=5,
                           json={"title": f"Cancelled Project {i}",
                                 "description": "Cancelled before start",
                                 "clientId": s4_client_id,
                                 "requiredSkills": ["Go"],
                                 "budget": 500.0})
        pid = rp.json()["id"] if rp.ok else None
        if pid:
            requests.patch(BASE + f"/api/projects/{pid}/cancel", timeout=5)

    rc = requests.get(BASE + f"/api/clients/{s4_client_id}", timeout=5).json()
    assert_field("completedProjects == 3",  rc.get("completedProjects"),  3)
    assert_field("cancelledProjects == 2",  rc.get("cancelledProjects"),  2)
    rep = round(rc.get("reputationScore", -1), 2)
    assert_field("reputationScore == 0.60", rep, 0.60)


# ─── SCENARIO 5 ───────────────────────────────────────────────────────────────
scenario(
    "Full Lifecycle with Freelancer Rating — end-to-end golden path",
    "LifeCycle Corp hires a freelancer, completes the project, then rates them.\n"
    "  The freelancer starts with 0 ratings. After two ratings (5 + 3) the\n"
    "  average must be exactly 4.0 and totalRatings must be 2."
)

r = check("Create LifeCycle Corp client", "post", "/api/clients",
          201, json={"name": "LifeCycle Corp", "email": "lifecycle@example.com"})
s5_client_id = r.json()["id"] if r and r.ok else None

r = check("Create freelancer StarDev", "post", "/api/freelancers",
          201, json={"name": "StarDev", "email": "fl-s5-stardev@example.com",
                     "skills": ["Kotlin", "Android"], "hourlyRate": 90.0})
s5_fl_id = r.json()["id"] if r and r.ok else None

if s5_client_id and s5_fl_id:
    r = check("Create Android project", "post", "/api/projects",
              201, json={"title": "Android Shopping App",
                         "description": "Native Kotlin e-commerce app",
                         "clientId": s5_client_id,
                         "requiredSkills": ["Kotlin", "Android"],
                         "budget": 8000.0})
    s5_proj_id = r.json()["id"] if r and r.ok else None

    if s5_proj_id:
        r = check("Submit bid", "post", "/api/bids",
                  201, json={"projectId": s5_proj_id, "freelancerId": s5_fl_id,
                             "amount": 7500.0, "message": "5 yrs Kotlin + Android"})
        s5_bid_id = r.json()["id"] if r and r.ok else None

        if s5_bid_id:
            check("Accept bid → project IN_PROGRESS", "patch",
                  f"/api/bids/{s5_bid_id}/accept", 200)

            rp = requests.get(BASE + f"/api/projects/{s5_proj_id}").json()
            assert_field("Project is IN_PROGRESS after accept",
                         rp.get("status"), "IN_PROGRESS")

            check("Complete project", "patch",
                  f"/api/projects/{s5_proj_id}/complete", 200)

            # Rate the freelancer twice
            check("Rate freelancer 5/5", "post",
                  f"/api/freelancers/{s5_fl_id}/ratings", 200,
                  json={"rating": 5})
            r = check("Rate freelancer 3/5", "post",
                      f"/api/freelancers/{s5_fl_id}/ratings", 200,
                      json={"rating": 3})

            if r and r.ok:
                fl_data = r.json()
                assert_field("totalRatings == 2",         fl_data.get("totalRatings"), 2)
                avg = round(fl_data.get("averageRating", -1), 1)
                assert_field("averageRating == 4.0",      avg, 4.0)

        # Verify client completed count increased
        rc = requests.get(BASE + f"/api/clients/{s5_client_id}").json()
        assert_field("Client completedProjects == 1", rc.get("completedProjects"), 1)


# ─── SCENARIO 6 ───────────────────────────────────────────────────────────────
scenario(
    "Concurrent Rating Storm — atomic $inc under parallel load",
    "100 rating requests (all value=1) are fired in parallel against a single\n"
    "  freelancer. The final totalRatings must equal exactly 100 and\n"
    "  averageRating must equal exactly 1.0 — proving no $inc update is lost."
)

import concurrent.futures

CONCURRENT_RATINGS = 100

r = requests.post(BASE + "/api/freelancers", timeout=5,
                  json={"name": "Storm Target", "email": "fl-s6-storm@example.com",
                        "skills": ["Go"], "hourlyRate": 80.0})
s6_fl_id = r.json()["id"] if r.ok else None

if s6_fl_id:
    def fire_rating(_):
        return requests.post(BASE + f"/api/freelancers/{s6_fl_id}/ratings",
                             json={"rating": 1}, timeout=10)

    with concurrent.futures.ThreadPoolExecutor(max_workers=CONCURRENT_RATINGS) as pool:
        futures = list(pool.map(fire_rating, range(CONCURRENT_RATINGS)))

    http_200s = sum(1 for resp in futures if resp.status_code == 200)
    print(f"  {Fore.WHITE}  {http_200s}/{CONCURRENT_RATINGS} HTTP 200s received{Style.RESET_ALL}")

    fl_final = requests.get(BASE + f"/api/freelancers/{s6_fl_id}", timeout=5).json()
    assert_field(f"totalRatings == {CONCURRENT_RATINGS} (no lost $inc)",
                 fl_final.get("totalRatings"), CONCURRENT_RATINGS)
    avg6 = round(fl_final.get("averageRating", -1), 1)
    assert_field("averageRating == 1.0 (all ratings were 1)",
                 avg6, 1.0)
else:
    print(f"  {FAIL}  Could not create storm-target freelancer — skipping scenario 6")
    results.append(False)
    results.append(False)


# ─── SCENARIO 7 ───────────────────────────────────────────────────────────────
scenario(
    "Concurrent Client Counter Storm — atomic $inc for completed & cancelled",
    "A single client owns 100 OPEN projects and 100 IN_PROGRESS projects.\n"
    "  All 100 cancel and all 100 complete PATCHes are fired in parallel.\n"
    "  cancelledProjects must equal 100, completedProjects must equal 100,\n"
    "  and reputationScore must equal exactly 0.50 — proving no $inc is lost."
)

CONCURRENT_PROJECTS = 100

r = requests.post(BASE + "/api/clients", timeout=5,
                  json={"name": "Counter Storm Corp", "email": "fl-s7-counterstorm@example.com"})
s7_client_id = r.json()["id"] if r.ok else None

# One freelancer per complete-project to avoid the 3-active-project capacity wall
s7_fl_ids = []
for i in range(CONCURRENT_PROJECTS):
    rw = requests.post(BASE + "/api/freelancers", timeout=5,
                       json={"name": f"S7 Worker {i:02d}",
                             "email": f"fl-s7-w-{i:02d}@example.com",
                             "skills": ["Scala"], "hourlyRate": 55.0})
    if rw.ok:
        s7_fl_ids.append(rw.json()["id"])

if s7_client_id and len(s7_fl_ids) == CONCURRENT_PROJECTS:
    # --- set up OPEN projects (cancel targets) ---
    s7_open_ids = []
    for i in range(CONCURRENT_PROJECTS):
        rp = requests.post(BASE + "/api/projects", timeout=5,
                           json={"title": f"Cancel-me {i}", "description": "to be cancelled",
                                 "clientId": s7_client_id,
                                 "requiredSkills": ["Scala"], "budget": 100.0})
        if rp.ok:
            s7_open_ids.append(rp.json()["id"])

    # --- set up IN_PROGRESS projects (complete targets): create → bid → accept ---
    s7_inprogress_ids = []
    for i, fl_id in enumerate(s7_fl_ids):
        rp = requests.post(BASE + "/api/projects", timeout=5,
                           json={"title": f"Complete-me {i}", "description": "to be completed",
                                 "clientId": s7_client_id,
                                 "requiredSkills": ["Scala"], "budget": 100.0})
        if not rp.ok:
            continue
        proj_id = rp.json()["id"]
        rb = requests.post(BASE + "/api/bids", timeout=5,
                           json={"projectId": proj_id, "freelancerId": fl_id,
                                 "amount": 90.0, "message": "storm worker"})
        if not rb.ok:
            continue
        bid_id = rb.json()["id"]
        ra = requests.patch(BASE + f"/api/bids/{bid_id}/accept", timeout=5)
        if ra.ok:
            s7_inprogress_ids.append(proj_id)

    print(f"  {Fore.WHITE}  {len(s7_open_ids)} OPEN projects ready to cancel, "
          f"{len(s7_inprogress_ids)} IN_PROGRESS projects ready to complete{Style.RESET_ALL}")

    # --- fire all cancels and completes in parallel ---
    def fire_cancel(proj_id):
        return requests.patch(BASE + f"/api/projects/{proj_id}/cancel", timeout=10)

    def fire_complete(proj_id):
        return requests.patch(BASE + f"/api/projects/{proj_id}/complete", timeout=10)

    with concurrent.futures.ThreadPoolExecutor(max_workers=CONCURRENT_PROJECTS * 2) as pool:
        cancel_futs  = list(pool.map(fire_cancel,   s7_open_ids))
        complete_futs = list(pool.map(fire_complete, s7_inprogress_ids))

    cancel_200s  = sum(1 for r in cancel_futs  if r.status_code == 200)
    complete_200s = sum(1 for r in complete_futs if r.status_code == 200)
    print(f"  {Fore.WHITE}  {cancel_200s}/{len(s7_open_ids)} cancel 200s, "
          f"{complete_200s}/{len(s7_inprogress_ids)} complete 200s{Style.RESET_ALL}")

    c7 = requests.get(BASE + f"/api/clients/{s7_client_id}", timeout=5).json()
    assert_field(f"cancelledProjects == {CONCURRENT_PROJECTS} (no lost $inc)",
                 c7.get("cancelledProjects"), CONCURRENT_PROJECTS)
    assert_field(f"completedProjects == {CONCURRENT_PROJECTS} (no lost $inc)",
                 c7.get("completedProjects"), CONCURRENT_PROJECTS)
    rep7 = round(c7.get("reputationScore", -1), 2)
    assert_field("reputationScore == 0.50", rep7, 0.50)
else:
    print(f"  {FAIL}  Could not create client/freelancer for scenario 7 — skipping")
    results.append(False)
    results.append(False)
    results.append(False)


# ─── SUMMARY ──────────────────────────────────────────────────────────────────
passed = sum(results)
total = len(results)
failed = total - passed
print(f"\n{Style.BRIGHT}{'═'*70}")
if failed == 0:
    print(f"  {Fore.GREEN}{Style.BRIGHT}ALL {total} CHECKS PASSED ✓{Style.RESET_ALL}")
else:
    print(f"  {Fore.GREEN}{passed} passed{Style.RESET_ALL}  |  {Fore.RED}{failed} failed{Style.RESET_ALL}  |  {total} total")
print(f"{Style.BRIGHT}{'═'*70}{Style.RESET_ALL}\n")

sys.exit(0 if failed == 0 else 1)

