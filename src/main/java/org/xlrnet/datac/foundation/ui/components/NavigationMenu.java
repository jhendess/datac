package org.xlrnet.datac.foundation.ui.components;

import com.vaadin.server.FontAwesome;
import com.vaadin.server.ThemeResource;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.themes.ValoTheme;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.xlrnet.datac.Application;
import org.xlrnet.datac.BuildInformation;
import org.xlrnet.datac.foundation.ui.ViewType;
import org.xlrnet.datac.administration.domain.User;
import org.xlrnet.datac.session.services.UserService;

import javax.annotation.PostConstruct;

/**
 * A responsive menu component providing user information and the controls for
 * primary navigation between the views.
 */
@UIScope
@SpringComponent
@SuppressWarnings({"serial", "unchecked"})
public final class NavigationMenu extends CustomComponent {

    public static final String ID = "dashboard-menu";

    private static final String STYLE_VISIBLE = "valo-menu-visible";

    private MenuItem settingsItem;

    private final UserService userService;

    private final BuildInformation buildInformation;

    @Autowired
    public NavigationMenu(UserService userService, BuildInformation buildInformation) {
        this.userService = userService;
        this.buildInformation = buildInformation;
    }

    @Override
    public void attach() {
        super.attach();
        // Call buildContent() only when the session is already available to avoid errors
        setCompositionRoot(buildContent());
    }

    @PostConstruct
    private void init() {
        setPrimaryStyleName("valo-menu");
        setId(ID);
        setSizeUndefined();
    }

    private Component buildContent() {
        final CssLayout menuContent = new CssLayout();
        menuContent.addStyleName("sidebar");
        menuContent.addStyleName(ValoTheme.MENU_PART);
        menuContent.addStyleName("no-vertical-drag-hints");
        menuContent.addStyleName("no-horizontal-drag-hints");
        menuContent.setWidth(null);
        menuContent.setHeight("100%");

        menuContent.addComponent(buildTitle());
        menuContent.addComponent(buildUserMenu());
        menuContent.addComponent(buildToggleButton());
        menuContent.addComponent(buildMenuItems());
        menuContent.addComponent(buildVersionInformation());
        return menuContent;
    }

    private Component buildVersionInformation() {
        VerticalLayout layout = new VerticalLayout();
        layout.setDefaultComponentAlignment(Alignment.BOTTOM_CENTER);
        Label versionLabel = new Label(buildInformation.getVersion());
        Label revisionLabel = new Label(buildInformation.getRevision());

        layout.addComponents(versionLabel, revisionLabel);
        layout.setStyleName("version-numbers");
        return layout;
    }

    private Component buildTitle() {
        Label logo = new Label(Application.APPLICATION_NAME);
        logo.setStyleName(ValoTheme.LABEL_H2);
        logo.setSizeUndefined();
        HorizontalLayout logoWrapper = new HorizontalLayout(logo);
        logoWrapper.setComponentAlignment(logo, Alignment.MIDDLE_CENTER);
        logoWrapper.addStyleName("valo-menu-title");
        return logoWrapper;
    }

    private User getCurrentUser() {
        return this.userService.getSessionUser();
    }

    private Component buildUserMenu() {
        final MenuBar settings = new MenuBar();
        settings.addStyleName("user-menu");
        settingsItem = settings.addItem("", new ThemeResource(
                "img/profile-pic-300px.jpg"), null);
        updateUserName();
        /*settingsItem.addItem("Edit Profile", new Command() {
            @Override
            public void menuSelected(final MenuItem selectedItem) {
                ProfilePreferencesWindow.open(user, false);
            }
        });
        settingsItem.addItem("Preferences", new Command() {
            @Override
            public void menuSelected(final MenuItem selectedItem) {
                ProfilePreferencesWindow.open(user, true);
            }
        });
        settingsItem.addSeparator();*/
        settingsItem.addItem("Sign Out", (MenuBar.Command) selectedItem -> {
            userService.logout();
        });
        return settings;
    }

    private Component buildToggleButton() {
        Button valoMenuToggleButton = new Button("Menu", new ClickListener() {
            @Override
            public void buttonClick(final ClickEvent event) {
                if (getCompositionRoot().getStyleName().contains(STYLE_VISIBLE)) {
                    getCompositionRoot().removeStyleName(STYLE_VISIBLE);
                } else {
                    getCompositionRoot().addStyleName(STYLE_VISIBLE);
                }
            }
        });
        valoMenuToggleButton.setIcon(FontAwesome.LIST);
        valoMenuToggleButton.addStyleName("valo-menu-toggle");
        valoMenuToggleButton.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        valoMenuToggleButton.addStyleName(ValoTheme.BUTTON_SMALL);
        return valoMenuToggleButton;
    }

    private Component buildMenuItems() {
        CssLayout menuItemsLayout = new CssLayout();
        menuItemsLayout.addStyleName("valo-menuitems");

        for (ViewType viewType : ViewType.values()) {
            MenuItemButton menuItemButton = new MenuItemButton(viewType);
            menuItemsLayout.addComponent(menuItemButton);
        }

        return menuItemsLayout;
    }

    private void updateUserName() {
        User user = getCurrentUser();
        settingsItem.setText(StringUtils.stripToEmpty(user.getFirstName()) + " " + StringUtils.stripToEmpty(user.getLastName()));
    }
}
