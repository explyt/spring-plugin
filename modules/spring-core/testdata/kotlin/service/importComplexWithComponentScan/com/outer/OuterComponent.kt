package com.outer;

import com.outerimport.OuterImport;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

@Import(com.outerimport.OuterImport::class)
@Component
class OuterComponent
