package com;

import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;

import javax.swing.text.html.parser.Entity;

@Async
public final class GeneratorServiceImpl implements GeneratorService {
    @Override
    public void generatorService(final Entity entity) {}

    @org.springframework.transaction.annotation.Transactional
    public final void key(){}
}

interface GeneratorService {
    void generatorService(Entity entity);
}

class CachingService{
    @Caching
    public final void caching(){
    }
}

@Configuration
class FinalConfiguration {
    @Bean
    final FinalObject getObj() {
        return new FinalObject();
    }
}

class FinalObject {}