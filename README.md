# mgrep
> 为解决grep搜索时只能按行匹配
>虽然grep -A B C 可以带上下文，但是关键词命中多行，加了上下文，返回结果会干扰分析，特别是程序日志查阅时

使用方法：
 * mgrep '关键词1 关键词2' -h '[fm-workorder-server'  -f 文件名
 * mgrep '关键词1 关键词2' -f 文件名
 * mgrep '关键词1 关键词2' 文件名
