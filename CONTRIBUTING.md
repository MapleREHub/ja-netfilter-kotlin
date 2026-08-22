# 贡献指南

## Conventional Commits 规范

本项目使用 [Conventional Commits](https://www.conventionalcommits.org/) 规范来自动管理版本号和 changelog。
提交信息必须按以下格式：

```
<类型>(<作用域>): <描述>

[可选的详细描述]

[可选的脚注]
```

### 类型（决定版本号）

| 类型 | 版本影响 | 说明 |
|------|---------|------|
| `feat` | minor (0.x.0) | 新功能 |
| `fix` | patch (0.0.x) | Bug 修复 |
| `perf` | patch | 性能优化 |
| `refactor` | 无 | 代码重构 |
| `docs` | 无 | 文档变更 |
| `test` | 无 | 测试相关 |
| `build` | 无 | 构建系统 |
| `ci` | 无 | CI/CD |
| `chore` | 无 | 杂项（默认不加入 changelog） |
| `revert` | patch | 回退提交 |

### 重大变更

使用 `feat!` / `fix!` 或 `BREAKING CHANGE:` 脚注触发 major 版本：

```bash
feat!: redesign plugin loader API
# 或
feat: redesign plugin loader API

BREAKING CHANGE: PluginEntry.init signature has changed
```

### 示例

```bash
git commit -m "feat(dns): add support for IP-based rules"
git commit -m "fix(power): handle negative BigInteger correctly"
git commit -m "docs: update reverse-analysis with new findings"
git commit -m "chore: bump gradle wrapper to 8.14"
```

## 发版流程

本项目使用 [release-please](https://github.com/googleapis/release-please) 自动发版。

### 自动流程

1. 开发者提交 Conventional Commits 到 `main` 分支
2. release-please 自动创建/更新 "Release PR"
3. 维护者审核 Release PR（版本号、changelog）
4. **合并 Release PR** → 自动创建 GitHub Release 并上传所有构建产物

### 首次发版（手动触发）

由于 release-please 需要通过 PR 工作，所以首次发版需要手动触发 workflow：

1. 进入 GitHub Actions 页面
2. 选择 "Release Please" workflow
3. 点击 "Run workflow"
4. 选择 main 分支执行
5. release-please 会创建第一个 Release PR
6. 审核并合并 PR

## 本地测试

```bash
# 编译
./gradlew clean build

# 生成 fat jar
./gradlew :ja-netfilter:fatJar

# 运行
java -jar ja-netfilter/build/libs/ja-netfilter-2.2.0-all.jar
```

## 代码规范

- Kotlin 官方代码风格
- 每个类都有中文注释说明
- 公开 API 必须有文档注释
- 单元测试覆盖核心逻辑

## 提交流程

1. Fork 项目
2. 创建特性分支 (`git checkout -b feat/amazing-feature`)
3. 提交更改 (`git commit -m "feat: add amazing feature"`)
4. 推送到分支 (`git push origin feat/amazing-feature`)
5. 创建 Pull Request