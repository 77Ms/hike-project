package com.example.springboot.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.entity.Word;
import com.example.springboot.mapper.WordMapper;
import com.example.springboot.service.IWordService;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 */
@Service
public class WordServiceImpl extends ServiceImpl<WordMapper, Word> implements IWordService {
    
}
