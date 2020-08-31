package com.servotronix.mcwebserver;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import java.io.IOException;
import java.io.StringWriter;
import spark.ModelAndView;
import spark.TemplateEngine;


public class FreeMarkerEngine extends TemplateEngine {
    
    private Configuration configuration;
    
    public FreeMarkerEngine() {
        this.configuration = createDefaultConfiguration();
    }
    
    public FreeMarkerEngine(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String render(ModelAndView mav) {
        try {
            StringWriter stringWriter = new StringWriter();
            Template template = configuration.getTemplate(mav.getViewName());
            template.process(mav.getModel(), stringWriter);
            return stringWriter.toString();
        } catch (IOException | TemplateException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    private Configuration createDefaultConfiguration() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_26);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setClassForTemplateLoading(getClass(), "/"); // searches for templates inside resources/
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        return cfg;
    }
    
    
    
}
