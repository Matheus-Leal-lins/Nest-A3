package br.com.nest.ui;

import br.com.nest.model.ChildProfile;
import br.com.nest.service.NestService;

import java.awt.*;

/** O que cada tela precisa saber da janela principal. */
interface AppContext {
    NestService service();

    /** Perfil selecionado no topo da janela, ou {@code null} se ainda não existe nenhum. */
    ChildProfile selectedChild();

    /** Recarrega todas as telas (após qualquer alteração de dados). */
    void refreshAll();

    Component frame();
}
