import pandas as pd

WEIGHT_ISSUE = 0.5
WEIGHT_CODE = 0.4
WEIGHT_CODE_ADD = 0.8
WEIGHT_CODE_DEL = 0.2
WEIGHT_COMMIT = 0.1

df = pd.read_excel("group12_for_health_contribution.xlsx")

total_issues = df["Issue数"].sum()
total_commits = df["提交数"].sum()
total_adds = df["增加行数"].sum()
total_dels = df["删除行数"].sum()

def ratio(value, total):
    return value / total if total else 0

issue_component = WEIGHT_ISSUE * df["Issue数"].apply(lambda x: ratio(x, total_issues))
commit_component = WEIGHT_COMMIT * df["提交数"].apply(lambda x: ratio(x, total_commits))
add_component = WEIGHT_CODE_ADD * df["增加行数"].apply(lambda x: ratio(x, total_adds))
del_component = WEIGHT_CODE_DEL * df["删除行数"].apply(lambda x: ratio(x, total_dels))
code_component = WEIGHT_CODE * (add_component + del_component)

df["贡献度(%)"] = (issue_component + commit_component + code_component) * 100
df["贡献度(%)"] = df["贡献度(%)"].round(2)

records = df.to_dict(orient="records")
for row in records:
    print(row)
df.to_excel("group12_for_health_contribution_with_score.xlsx", index=False)