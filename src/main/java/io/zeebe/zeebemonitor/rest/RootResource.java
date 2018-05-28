package io.zeebe.zeebemonitor.rest;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import io.zeebe.zeebemonitor.repository.ConfigurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Component
@RestController
@RequestMapping("/")
public class RootResource
{
    @Autowired
    private ConfigurationRepository configurationRepository;

    @RequestMapping("/")
    public String get(HttpServletResponse httpResponse) throws IOException
    {
        if (configurationRepository.getConfiguration().isPresent())
        {
            httpResponse.sendRedirect("/index.html");
        }
        else
        {
            httpResponse.sendRedirect("/setup.html");
        }
        return null;
    }
}
