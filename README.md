# Smart Input

一款为中文开发者设计的 IntelliJ IDEA 输入法智能切换插件。根据编辑器上下文自动切换中英文输入法，告别手动切换的烦恼。

## ✨ 功能特性

| 场景 | 行为 | 说明 |
|------|------|------|
| 代码区域 | → 英文 | 光标在普通代码中时自动切换到英文输入法 |
| 注释区域 | → 中文 | 单行注释、多行注释、文档注释中自动切换到中文 |
| Commit 信息 | → 中文 | Git 提交编辑器中自动切换到中文 |
| Terminal | → 英文 | 终端窗口获得焦点时自动切换到英文 |
| IdeaVim 命令模式 | → 英文 | 进入 Vim 命令模式时自动切换到英文（需安装 IdeaVim） |
| 光标颜色指示 | 可视化 | 蓝色=英文、红色=中文、橙色=Caps Lock |

## 📋 系统要求

- IntelliJ IDEA 2024.1 及以上版本
- macOS 系统
- Python 3（macOS 自带，用于调用系统 API 切换输入法）

> 插件默认使用 macOS Carbon API 切换输入法，无需安装额外工具，无需辅助功能权限。如果已安装 [macism](https://github.com/laishulu/macism)，会优先使用。

## 🚀 安装

1. 下载最新 release 中的 `smart-plugin.zip`
2. 在 IntelliJ IDEA 中打开 `Settings → Plugins → ⚙️ → Install Plugin from Disk...`
3. 选择下载的 zip 文件，重启 IDE

## ⚙️ 配置

在 `Settings → Tools → Smart Input` 中进行设置：

- **General** — 全局开关
- **Context Detection** — 独立开关各项上下文检测
- **Input Methods** — 自定义中英文输入法 ID（默认适配 macOS 系统自带输入法）
- **Cursor Indicator** — 自定义光标颜色指示器

### 常用输入法 ID

| 输入法 | ID |
|--------|-----|
| ABC (英文) | `com.apple.keylayout.ABC` |
| 简体拼音 | `com.apple.inputmethod.SCIM.ITABC` |
| 简体双拼 | `com.apple.inputmethod.Shuangpin` |

## ⌨️ 快捷键

`Ctrl + Alt + I` — 快速开关插件

## 🔧 开发

```bash
# 克隆项目
git clone https://github.com/thumb0520/idea-smart-input.git
cd idea-smart-input

# 运行插件（启动一个带插件的 IDE 实例）
./gradlew runIde

# 构建插件
./gradlew buildPlugin
```

## 📄 License

MIT
