为 Android Developer Tools 工程
修改前后的代码分别在old和new目录中。
除了书中所述的循环依赖外，还有以下循环依赖关系：

1. DBHelper <-> Records
解决：DBHelper 不应该依赖 Records，目前只要是需要识别 Records 中的 Record 结构的内容。
      把 Record 提到 Util中，另外 loadRecords 时返回一个 Record 的数组。

2. BlockGrp <-> GameArea
解决：BlockGrp依赖GameArea的 isPositionEmpty 函数，考虑放一些功能在 GameArea 中实现。
使用“降级”方法消除循环依赖

3. WaitingAreaArea <-> GameView
解决：WaitingAreaArea 依赖 GameView 的 isScreenLandScape。
考虑使用参数传入的办法

4. GameArea <-> GameView
解决：GameArea依赖 GameView 的 isScreenLandScape , setBackupDropGrp , dropAddedInGameArea, updateScore。
使用“降级”方法消除循环依赖

5. Block -> DBHelper
Block单向依赖于DBHelper，虽然没有循环依赖，但这个依赖不应该有，采用功能重新划分的方式避免了这一依赖。