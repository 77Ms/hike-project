package com.example.springboot.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
    
/**
 * 
 * 敏感词过滤
 * 
 */
@Slf4j
@SuppressWarnings({ "rawtypes", "unchecked" })
@Component
public class DFAUtil {
   // 敏感词
   private static Set<String> WORDS = new HashSet<String>();
   // 由敏感词生成的字树
   private static Map<String, Map> TREE = new ConcurrentHashMap<String, Map>();
   /* 在树当中标志一个词的结束 */
   public static final String TREE_END_KEY = "^";
   // 敏感词value标记
   public static final String WORD_VALUE = "v";
   // 敏感词长度标记
   public static final String WORD_LENGTH = "l";
   // 默认替换符
   public static final char DEFAULT_REPLACEMENT = '*';
   // 默认起始标记
   public static final String DEFAULT_START_TAG = "<font color=\"red\">";
   // 默认结束标记
   public static final String DEFAULT_END_TAG = "</font>";

   /**
    * 
    * 添加敏感词
    * 
    * @author hymer
    * @param words
    */
   public void addSensitiveWords(String[] words) {
      if (words == null || words.length == 0) {
         return;
      }
      check(words);
      addWords(words);
   }

   /**
    * 
    * 找出文本中的敏感词
    * 
    * @author hymer
    * @param text
    * @return
    */
   public Set<String> find(String text) {
      return new TextAnalysis().analysis(TREE, text);
   }

   /**
    * 替换文本中的敏感词
    * 
    * @param text
    *            含敏感词的文本
    * @param replacement
    *            替换字符
    * @return
    */
   public String replace(String text, Character replacement) {
      return new TextAnalysis().replace(TREE, text, replacement);
   }

   /**
    *
    * 过滤文本，并标记出敏感词，默认使用HTML中红色font标记
    *
    * @author hymer
    * @param text
    * @return
    */
   public String filter(String text) {
      return new TextAnalysis().mark(TREE, text, DEFAULT_START_TAG, DEFAULT_END_TAG);
   }


   private void check(String... words) {
      for (String word : words) {
         if (word != null && word.contains(TREE_END_KEY)) {
            throw new RuntimeException("包含非法字符：" + TREE_END_KEY);
         }
      }
   }

   private void addWords(String... sensitiveWords) {
      for (String word : sensitiveWords) {
         if (word != null && !word.trim().equals("")) {
            word = word.trim();
            int len = word.length();
            if (len > 1024) {
               throw new RuntimeException("敏感词太长[最长1024]!");
            }
            // 添加该词，如果未重复，则添加到tree
            if (WORDS.add(word)) {
               TreeGenerator.addWord2Tree(TREE, word);
            }
         }
      }
   }

   @SuppressWarnings("unused")
   private void printTree(Map<String, Map> wordTree, int level) {
      if (wordTree == null || wordTree.isEmpty()) {
         return;
      }
      Iterator<String> it = wordTree.keySet().iterator();
      while (it.hasNext()) {
         String next = it.next();
         Object tmp = wordTree.get(next);
         if (tmp instanceof Map) {
            printTree((Map) tmp, level + 1);
         }
      }
   }

}