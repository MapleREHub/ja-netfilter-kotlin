# 发版操作指南

> 本文档说明如何在新推送的 GitHub 项目上完成首次发版和后续发版流程。

## 一、推送状态

✅ 项目已成功推送到 GitHub：

- 仓库地址：`git@github.com:MapleREHub/ja-netfilter-kotlin.git`
- 主分支：`main`
- 提交历史：
  ```
  580b7a9 docs(readme): add quick install instructions and release workflow info
  08a987c feat(ci): add GitHub Actions CI and release-please workflows
  55a1009 feat: initial Kotlin reverse-engineered implementation of ja-netfilter
  ```

## 二、首次发版流程

### 步骤 1：手动触发 Release Please workflow

由于 release-please 通过 PR 工作，而首次发版没有现成的 PR，**必须手动触发 workflow**：

1. 打开 GitHub 仓库页面：`https://github.com/MapleREHub/ja-netfilter-kotlin`
2. 点击顶部的 **Actions** 标签
3. 在左侧找到 **"Release Please"** workflow
4. 点击右边的 **"Run workflow"** 按钮
5. 选择 **`main`** 分支
6. 点击绿色 **"Run workflow"** 按钮

![### 步骤 2：等待 workflow 完成

workflow 会执行以下操作：
1. ✅ Checkout 代码
2. ✅ 设置 JDK 17
3. ✅ 构建项目（所有 jars）
4. ✅ 准备 release 制品
5. ✅ 调用 release-please-action@v4 创建 Release PR

### 步骤 3：审核 Release PR

release-please 会创建一个标题类似 `chore(main): release 2.2.0` 的 PR，包含：
- 推荐的版本号（基于 Conventional Commits 分析）
- 自动生成的 CHANGELOG.md
- 已修改的 `gradle.properties` 版本号

**审核要点**：
- [ ] 版本号是否正确
- [ ] CHANGELOG 是否准确
- [ ] 所有 commit 都被正确分类

### 步骤 4：同意并合并 PR 🎉

**这是关键步骤！** 当您同意并合并 Release PR 时，release-please 会自动：

1. ✅ 创建 Git tag (`v2.2.0`)
2. ✅ 创建 GitHub Release（包含所有 jar 文件）
3. ✅ 上传所有构建产物（ja-netfilter.jar + 7个插件 jar + 配置文件 + 脚本 + 文档）
4. ✅ 完成首次正式发版

## 三、后续发版流程

合并 Release PR 后，每次新的符合 Conventional Commits 的 commit 推送到 main 分支时：

1. 🤖 release-please 自动检测到新提交
2. 📝 自动创建/更新 Release PR
3. 🏷️ 版本号根据 commit 类型自动 bump：
   - `feat:` → minor (`2.2.0` → `2.3.0`)
   - `fix:` → patch (`2.2.0` → `2.2.1`)
   - `feat!:` → major (`2.2.0` → `3.0.0`)
4. ✅ 您只需审核并合并 PR 即可完成发版

## 四、用户安装方式

发版后，用户可通过以下方式一键安装：

**Linux / macOS:**
```bash
curl -fsSL https://raw.githubusercontent.com/MapleREHub/ja-netfilter-kotlin/main/scripts/install-from-release.sh | bash
```

**Windows PowerShell:**
```powershell
iwr https://raw.githubusercontent.com/MapleREHub/ja-netfilter-kotlin/main/scripts/install-from-release.ps1 -OutFile install.ps1
.\install.ps1
```

**Windows CMD:**
```cmd
curl -fsSL https://raw.githubusercontent.com/MapleREHub/ja-netfilter-kotlin/main/scripts/install-from-release.bat -o install.bat
install.bat
```

这些脚本会：
1. 自动从 GitHub API 获取最新版本号
2. 下载最新 Release 中的 `ja-netfilter.jar`（如不存在则本地构建）
3. 下载所有插件 jar
4. 部署配置和脚本到当前目录

## 五、CI 验证

推送后，`ci.yml` workflow 会自动触发：
- 🐧 Linux 编译
- 🪟 Windows 编译
- 🍎 macOS 编译
- 📦 上传构建产物为 artifact（30天保留）

可以在 Actions 页面查看构建状态。

## 六、文件清单

新增的关键文件：
- `.github/workflows/ci.yml` - CI 工作流
- `.github/workflows/release-please.yml` - 自动发版工作流
- `.github/release-please-manifest.json` - 版本清单
- `release-please-config.json` - release-please 配置
- `CONTRIBUTING.md` - Conventional Commits 规范
- `scripts/install-from-release.sh` - Bash 一键安装
- `scripts/install-from-release.ps1` - PowerShell 一键安装
- `scripts/install-from-release.bat` - Batch 一键安装
- `docs/SETUP.md` - 本文档

## 七、下一步行动清单

- [ ] 打开 GitHub 仓库的 Actions 页面
- [ ] 手动运行 "Release Please" workflow
- [ ] 等待 release-please 创建 Release PR
- [ ] **审核 PR 内容**
- [ ] **同意并合并 Release PR** ← 触发正式发版
- [ ] 检查 GitHub Releases 页面确认发布成功
- [ ] 测试一键安装脚本

## 八、常见问题

### Q: release-please PR 没有自动创建？

A: 检查以下几点：
1. CI workflow 是否成功（`.github/workflows/ci.yml`）
2. main 分支是否有正确的 `gradle.properties` 版本号
4. workflow 日志中是否有错误

### Q: 合并 PR 后没有创建 Release？

A: 检查 GitHub Actions 日志：
1. 确认 release-please 步骤成功
2. 检查 `permissions: contents: write` 是否在 workflow 中配置

### Q: 想手动控制版本号？

A: 编辑 `release-please-config.json` 中的 `initial-version` 字段，或使用 git tag 手动触发。

### Q: 一键安装脚本失败？

A: 检查：
1. 网络连接是否正常
2. 是否能访问 `api.github.com`
3. Release 是否已经发布（脚本需要 Release 存在）