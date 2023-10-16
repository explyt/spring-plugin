package com;

import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.text.html.parser.Entity;

@Async
public final class GeneratorServiceImpl implements GeneratorService {
    @Override
    public void GeneratorService(final Entity entity) {}

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public final void key(){
    }
}

interface GeneratorService {
    void GeneratorService(Entity entity);
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