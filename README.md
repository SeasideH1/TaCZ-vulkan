# TaCZ-vulkan

## ⚠ AI大分警告！⚠ 除了这段话全是AI写的，纯AI无人工，全程GPT和它的降智模型，搞出来图一乐。

基于 [Timeless and Classics Guns Zero (TACZ)](https://github.com/MCModderAnchor/TACZ) 的**非官方、实验性 Fabric 移植**，面向 Minecraft `26.4-snapshot-3` 的原生渲染接口与 Vulkan 后端。与上游项目、Mojang、Microsoft 无隶属或背书关系。

当前源码包含 candidate18 的枪口火焰、枪包数据兼容和高模渲染优化改动。**尚非稳定版，不承诺所有枪包可用，也没有实测帧率提升结论。** 请向[本仓库 Issues](https://github.com/SeasideH1/TaCZ-vulkan/issues)反馈移植问题。

## 支持范围

| 项目 | 当前范围 |
| --- | --- |
| Minecraft | 仅 `26.4-snapshot-3`，Fabric 版本谓词为 `26.4-alpha.3` |
| 加载器 | Fabric Loader `0.19.5`，Fabric API `0.162.2+26.4` 为构建基线 |
| Java | JDK / Java 25；其他版本未验证 |
| 上游基线 | TACZ `1.1.8-hotfix2`，提交 `b482eff8c94a733ac8d0910193fca3893954027c` |
| 内容 | TACZ 核心与原版默认枪包；外部枪包按具体格式逐项兼容 |
| 渲染 | 目标版本原生渲染接口；历史候选有 Windows / Linux Vulkan 运行记录 |
| 当前验证 | candidate18 曾完成 715 个 Java 源文件编译及 JAR 结构检查；未做该候选的游戏、画面或性能验收 |
| 多人 / 服务端 | 存在历史服务端启动检查；完整多人登录与同步、长期运行未验收 |

不支持直接安装到 Forge / NeoForge、Minecraft 1.20.1 或其他快照。不保证 OpenGL、其他 GPU / 驱动与操作系统的表现。`optional-integrations/` 中的 Accelerated Rendering、Carry On、Cloth Config、Controllable、JEI、KubeJS、Oculus、OptiFine、Shoulder Surfing 仅保存源码，未编译或注册到运行模组。

## 已实现的移植改动

- 枪口火焰改用每帧姿态快照；最终视觉效果仍待验收。
- 旧枪包配方路径、物品 / 标签与结果数据适配；支持小数射速。运行时转换不改写用户枪包文件。
- 静态局部几何缓存、打包顶点数据和有界 GPU 缓冲缓存，减少重复 CPU 处理与上传。
- 合并满足相同材质、遮罩、深度等条件的相邻绘制；保持遮罩与需要排序的绘制边界。
- 为世界内第三人称、掉落物与固定展示添加小细节 LOD；第一人称和 GUI 保持完整细节。`SmallDetailLodDistance` 默认 16 格，`SmallDetailLodPixels` 默认 0.5 像素，任一设为 0 可关闭该细节裁剪。
- 连续绘制复用 render pass；瞄具遮罩使用缓冲池并减少复制。

以上是实现内容，不是已测得的性能收益。配置及实现细节以源码为准。

## 已知问题

- 部分配件无法出现在枪械候选列表的问题尚未全面定位。单一 M4A1 + ACOG 检查通过不代表所有组合正常。
- 保留上游弹匣卸载时的部分弹药返还缺陷；FN Evolys / M249 与部分扩容弹匣组合可能少返弹药。
- 不承诺任意第三方枪包、音效资源、脚本、组合瞄具或其他模组联动兼容。
- candidate17 / 18 的 GPU 缓存、批处理与 LOD 尚无完整画面回归、F3+T 重载验收或有效性能对照。

## 构建

需要 Python 3.12+、JDK 25 和官方依赖下载网络。使用仓库根目录执行下列命令；`JAVA_HOME` 应指向你的 JDK 25：

```powershell
python reproduction/rehydrate.py --fetch --local-jdk
python toolchains/prepare-fabric-compile-api.py --java-home "$env:JAVA_HOME"
python toolchains/direct-build.py forge-release-port --java-home "$env:JAVA_HOME"
python toolchains/package-runtime-candidate.py
```

Linux 将最后两个带 JDK 参数的命令中的 `"$env:JAVA_HOME"` 改为 `"$JAVA_HOME"`。完整说明见 [reproduction/README.md](reproduction/README.md)。构建输出位于 `forge-release-port/build/direct/<时间戳>/`，最终带 `runtime-candidate-<hash>.jar` 后缀的文件包含所需的 9 个纯 Java 库；普通开发 JAR 不包含这些库。打包程序会检查编译后的源码 / 资源漂移与 JAR 结构。

Gradle 配置也保留在仓库中，但本次候选采用上面的直接构建流程，不能把 Gradle 构建视作已验证的等价产物。历史原生 API 检查源码位于 `forge-release-port/src/test`；部分脚本依赖未发布的旧场景夹具，不作为开箱即用的自动验收套件。

本次公开源码已在 Windows / JDK 25 上重新完成 715 个 Java 文件编译及运行候选 JAR 打包；源码、资源和产物哈希见 [publication-build.json](reproduction/publication-build.json)。未启动游戏、运行画面回归或性能测试。

使用时在独立游戏实例安装上述确切版本的 Fabric、Fabric API 和构建生成的模组；不要同时安装原版 TACZ。第三方枪包需自行取得并遵守其许可。仓库不含 Minecraft、游戏账号、存档或外部枪包。

## 许可与署名

本仓库**不是所有文件统一适用 GPL**：

- TACZ 程序代码按上游 **GNU GPL v3** 许可，见 [LICENSE](LICENSE)。修改版保留署名、修改声明与对应源码；不得以本项目名义撤销 GPL 赋予代码接收者的权利。
- 上游原始模型、纹理、动画、音效及默认枪包美术资源按 **CC BY-NC-ND 4.0**。共享原始资源需遵守署名、非商业、禁止分享演绎作品等条款；代码的 GPL 不授予这些资源相同的使用权限。详见 [ASSET_LICENSE.md](ASSET_LICENSE.md)。
- SimpleBedrockModel 派生接口保留 LGPL 声明；捆绑库各自遵循 Apache-2.0、MIT、LGPL 等原许可，完整文本、来源及对应源码见 [第三方许可](forge-release-port/THIRD_PARTY_LICENSES.md) 和 `reproduction/dependency-sources/`。

上游程序作者：`286799714`、`TartaricAcid`、`F1zeiL`、`xjqsh`、`ClumsyAlien`。美术作者：`NekoCrane`、`Receke`、`Pos_2333`。其他原始文件内署名亦保留。移植维护：SeasideH1。修改范围与日期见 [NOTICE.md](NOTICE.md)。

English: This is an unofficial experimental Fabric port of TACZ for Minecraft 26.4-snapshot-3. It is not a stable release or an upstream-endorsed project. Code, original artwork and third-party dependencies have different license terms. Performance gains and full gunpack compatibility have not been established.
