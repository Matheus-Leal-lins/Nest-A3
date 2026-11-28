package br.com.nest.ui;

import java.awt.Component;

import br.com.nest.model.ChildProfile;
import br.com.nest.service.NestService;


interface AppContext {
    NestService service();


    ChildProfile selectedChild();


    void refreshAll();

    Component frame();
}
