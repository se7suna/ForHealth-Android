# Memory 提示词：GitLab 项目开发工作流

## 项目上下文

- **项目名称**: for_health
- **平台**: GitLab
- **协作模式**: 团队开发
- **当前角色**: 开发工程师
- **工具**: 使用 GitLab MCP 进行项目管理
---

## 核心工作流程

### 1. 需求分析与任务规划

- 读取并理解 SRS 文档中的 use case
- 分析被分配的 issue 需求和验收标准
- 将 use case 转化为具体开发任务
- 评估任务复杂度和依赖关系

### 2. 分支管理策略

**分支命名规范：**
- `feature/issue-{issue_id}-{short-description}`
- `bugfix/issue-{issue_id}-{short-description}`
- `hotfix/issue-{issue_id}-{short-description}`

### 3. 开发执行流程

1. **创建分支**: 基于当前 issue 创建功能分支
2. **代码开发**: 实现 issue 要求的功能
3. **本地测试**: 在提交前进行基础验证
4. **提交代码**: 使用规范的 commit message

**Commit Message 规范：**
```
feat: [ISSUE-{id}] {description}
fix: [ISSUE-{id}] {description}
docs: [ISSUE-{id}] {description}
```

### 4. CI/CD 流水线管理

- **Runner 配置**: 创建和维护 GitLab Runner
- **Pipeline 脚本**: 编写 `.gitlab-ci.yml` 包含：
  - 代码质量检查
  - 单元测试执行
  - 集成测试验证
  - 构建验证
- **测试自动化**: 确保每次提交自动运行测试套件

### 5. 合并请求流程

1. 创建 Merge Request 到目标分支（develop/main）
2. 添加相关 reviewer
3. 关联对应的 issue
4. 确保 CI 流水线通过
5. 解决代码审查反馈

---

## 具体能力要求

### GitLab MCP 集成能力

- ✅ 项目仓库操作（clone, fetch, pull）
- ✅ 分支管理（create, checkout, delete）
- ✅ 代码提交（add, commit, push）
- ✅ 合并请求管理（create, review, merge）
- ✅ Issue 跟踪和更新
- ✅ CI/CD 流水线配置

---

## 开发质量标准

- 代码符合项目编码规范
- 包含必要的单元测试
- 通过所有自动化测试
- 文档更新（如有需要）
- 无回归错误

## 测试验证标准

- 单元测试覆盖率不降低
- 集成测试通过率 100%
- 无新的 linting 错误
- 构建成功完成
- 性能指标在可接受范围

---

## 关键检查点

- **开发前**: 确认 issue 理解正确，分支策略合适
- **开发中**: 定期提交，保持代码质量
- **提交前**: 本地测试通过，commit message 规范
- **MR 创建**: 描述清晰，关联正确，CI 触发
- **CI 结果**: 监控流水线状态，及时修复失败

---

## 沟通协作

- 在 issue 中更新进度
- MR 描述中说明变更内容
- 及时响应 review 意见
- 标注阻塞问题和风险