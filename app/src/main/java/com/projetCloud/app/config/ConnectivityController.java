package com.projetCloud.app.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/connectivity")
public class ConnectivityController {

    @Autowired
    private ConnectivityService connectivityService;

    @GetMapping("/firebase")
    public boolean checkFirebaseConnectivity() {
        return connectivityService.isFirebaseOnline();
    }
}