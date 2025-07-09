package cn.wangfeixiong.csearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Aho-Corasick 多模式字符串匹配算法工具类
 * 用于在文本中高效查找多个关键词的出现位置
 */
public class AhoCorasickMatcher {

    // 字典树节点类
    private static class TrieNode {
        // 子节点映射，字符到节点的映射
        Map<Character, TrieNode> children = new HashMap<>();
        // 失败指针，用于在匹配失败时跳转
        TrieNode fail;
        // 命中的关键词列表
        List<String> outputs = new ArrayList<>();
    }

    // 根节点
    private final TrieNode root;
    // 是否已经构建失败指针
    private boolean failureBuilt = false;

    /**
     * 构造函数，初始化根节点
     */
    public AhoCorasickMatcher() {
        this.root = new TrieNode();
    }

    /**
     * 添加关键词到字典树
     * @param keyword 关键词字符串
     */
    public void addKeyword(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return;
        }

        TrieNode current = root;
        for (char c : keyword.toCharArray()) {
            current.children.putIfAbsent(c, new TrieNode());
            current = current.children.get(c);
        }
        // 将关键词添加到叶子节点的输出列表
        current.outputs.add(keyword);
    }

    /**
     * 构建失败指针
     * 必须在添加所有关键词后调用
     */
    public void buildFailurePointer() {
        if (failureBuilt) {
            return;
        }

        Queue<TrieNode> queue = new LinkedList<>();

        // 初始化根节点的子节点的失败指针为根节点
        root.children.forEach((c, node) -> {
            node.fail = root;
            queue.add(node);
        });

        // BFS 遍历构建失败指针
        while (!queue.isEmpty()) {
            TrieNode current = queue.poll();

            current.children.forEach((c, childNode) -> {
                TrieNode failNode = current.fail;

                // 查找失败指针
                while (failNode != null && !failNode.children.containsKey(c)) {
                    failNode = failNode.fail;
                }

                childNode.fail = failNode != null ? failNode.children.get(c) : root;

                // 合并输出
                if (!childNode.fail.outputs.isEmpty()) {
                    childNode.outputs.addAll(childNode.fail.outputs);
                }

                queue.add(childNode);
            });
        }

        failureBuilt = true;
    }

    /**
     * 在文本中查找所有匹配的关键词
     * @param text 待匹配的文本
     * @return 匹配结果列表
     */
    public List<MatchResult> findAllMatches(String text) {
        if (!failureBuilt) {
            buildFailurePointer();
        }

        List<MatchResult> results = new ArrayList<>();
        TrieNode current = root;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // 查找下一个节点，如果不存在则使用失败指针跳转
            while (current != null && !current.children.containsKey(c)) {
                current = current.fail;
            }

            if (current == null) {
                current = root;
                continue;
            }

            current = current.children.get(c);

            // 收集所有匹配的关键词
            if (!current.outputs.isEmpty()) {
                for (String keyword : current.outputs) {
                    results.add(new MatchResult(i - keyword.length() + 1, i+1, keyword));
                }
            }
        }

        return results;
    }

    /**
     * 匹配结果类，包含匹配的起始位置、结束位置和关键词
     */
    public static class MatchResult {
        private final int start;
        private final int end;
        private final String keyword;

        public MatchResult(int start, int end, String keyword) {
            this.start = start;
            this.end = end;
            this.keyword = keyword;
        }

        /**
         * 获取匹配的起始位置（包含）
         * @return 起始位置
         */
        public int getStart() {
            return start;
        }

        /**
         * 获取匹配的结束位置（包含）
         * @return 结束位置
         */
        public int getEnd() {
            return end;
        }

        /**
         * 获取匹配的关键词
         * @return 关键词
         */
        public String getKeyword() {
            return keyword;
        }

        @Override
        public String toString() {
            return "MatchResult{" +
                "start=" + start +
                ", end=" + end +
                ", keyword='" + keyword + '\'' +
                '}';
        }
    }
}
