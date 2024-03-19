package com.outer;

import com.outerimport.OuterImport;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

@Import(OuterImport.class)
@Component
public class OuterComponent {
}
