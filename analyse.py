import requests
import pandas as pd

GITLAB_URL = "https://gitlab.com"
PROJECT_PATH = "tj-cs-swe/CS10102302-2025/group12/for_health"
TOKEN = "glpat-THfDoMOYZOOyvj4QA_bUBG86MQp1OmZ3ZjN2Cw.01.120kg2a94"

# 获取项目 ID
proj_resp = requests.get(
    f"{GITLAB_URL}/api/v4/projects/{requests.utils.quote(PROJECT_PATH, safe='')}",
    headers={"PRIVATE-TOKEN": TOKEN}
)
proj_resp.raise_for_status()
proj = proj_resp.json()
project_id = proj["id"]

# 统计 Issue 数
issues = []
page = 1
while True:
    resp = requests.get(
        f"{GITLAB_URL}/api/v4/projects/{project_id}/issues",
        headers={"PRIVATE-TOKEN": TOKEN},
        params={"per_page": 100, "page": page, "state": "all"}
    )
    resp.raise_for_status()
    data = resp.json()
    if not data:
        break
    issues.extend(data)
    page += 1

issue_count = {}
for i in issues:
    issue_name = i.get("description")
    assignee = i.get("assignee", {}) or {}
    name = assignee.get("name", "<unknown>")
    issue_count[name] = issue_count.get(name, 0) + 1


# 获取所有已合并 Merge Request 的提交
merged_mrs = []
page = 1
while True:
    resp = requests.get(
        f"{GITLAB_URL}/api/v4/projects/{project_id}/merge_requests",
        headers={"PRIVATE-TOKEN": TOKEN},
        params={"state": "merged", "per_page": 100, "page": page}
    )
    resp.raise_for_status()
    data = resp.json()
    if not data:
        break
    merged_mrs.extend(data)
    page += 1

all_commits = []
seen_commits = set()
for mr in merged_mrs:
    commits_resp = requests.get(
        f"{GITLAB_URL}/api/v4/projects/{project_id}/merge_requests/{mr['iid']}/commits",
        headers={"PRIVATE-TOKEN": TOKEN}
    )
    commits_resp.raise_for_status()
    mr_commits = commits_resp.json()
    for commit in mr_commits:
        sha = commit["id"]
        if sha in seen_commits:
            continue
        seen_commits.add(sha)
        all_commits.extend([commit])

# 统计提交次数
commit_count = {}
for c in all_commits:
    name = c.get("author_name", "<unknown>")
    commit_count[name] = commit_count.get(name, 0) + 1


# 2) 新增：统计代码增删行数
loc_stats = {}  # {author: {"add": X, "del": Y, "total": Z}}

for c in all_commits:
    sha = c["id"]
    name = c["author_name"]

    # 获取 commit 行数统计
    stats = requests.get(
        f"{GITLAB_URL}/api/v4/projects/{project_id}/repository/commits/{sha}",
        headers={"PRIVATE-TOKEN": TOKEN}
    ).json()
    
    adds = stats["stats"].get("additions", 0)
    dels = stats["stats"].get("deletions", 0)

    if name not in loc_stats:
        loc_stats[name] = {"add": 0, "del": 0, "total": 0}

    loc_stats[name]["add"] += adds
    loc_stats[name]["del"] += dels
    loc_stats[name]["total"] += adds + dels


# 合并进最终 DataFrame
rows = []
names = set(commit_count) | set(issue_count) | set(loc_stats)

for name in names:
    rows.append({
        "成员": name,
        "提交数": commit_count.get(name, 0),
        "Issue数": issue_count.get(name, 0),

        # 新增的行数统计
        "增加行数": loc_stats.get(name, {}).get("add", 0),
        "删除行数": loc_stats.get(name, {}).get("del", 0),
        "总变更行数": loc_stats.get(name, {}).get("total", 0),

        # 你原本的贡献度，同时可加入行数权重
        "总贡献度": (
            commit_count.get(name, 0) * 1 +
            issue_count.get(name, 0) * 2 +
            loc_stats.get(name, {}).get("total", 0) * 0.1  # 行数权重可自由调整
        )
    })

df = pd.DataFrame(rows)
df = df.sort_values(by="总贡献度", ascending=False)
df.to_excel("contribution_test.xlsx", index=False)

print("完成：输出文件为 contribution_test.xlsx")
