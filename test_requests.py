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
    for c in (r.json() if r.ok else []):
        if c.get("email") in {"hiring@broski.io", "projects@broski.io"}:
            requests.delete(BASE + f"/api/clients/{c['id']}", timeout=5)

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

