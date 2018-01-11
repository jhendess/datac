package org.xlrnet.datac.foundation.ui.components;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.Application;
import org.xlrnet.datac.foundation.configuration.BuildInformation;
import org.xlrnet.datac.session.domain.User;
import org.xlrnet.datac.session.services.UserService;
import org.xlrnet.datac.session.ui.components.UserProfileWindow;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ThemeResource;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.themes.ValoTheme;

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

    private final UserProfileWindow userProfileWindow;

    @Autowired
    public NavigationMenu(UserService userService, BuildInformation buildInformation, UserProfileWindow userProfileWindow) {
        this.userService = userService;
        this.buildInformation = buildInformation;
        this.userProfileWindow = userProfileWindow;
    }

    @Override
    public void attach() {
        super.attach();
        // Call buildContent() only when the session is already available to avoid errors
        setCompositionRoot(buildContent());
        User sessionUser = userService.getSessionUser();
        if (sessionUser != null && sessionUser.isPwChangeNecessary()) {
            userProfileWindow.open();
        }
    }

    @PostConstruct
    private void init() {
        setPrimaryStyleName("valo-menu");
        setId(ID);
        setHeight("100%");
        addStyleName("navigation-menu");
    }

    private Component buildContent() {
        final CssLayout menuContent = new CssLayout();
        menuContent.addStyleName("sidebar");
        menuContent.addStyleName(ValoTheme.MENU_PART);
        menuContent.addStyleName("no-vertical-drag-hints");
        menuContent.addStyleName("no-horizontal-drag-hints");
        menuContent.setHeight("100%");

        menuContent.addComponent(buildTitle());
        menuContent.addComponent(buildUserMenu());
        menuContent.addComponent(buildToggleButton());
        menuContent.addComponent(buildMenuItems());
        menuContent.addComponent(buildVersionInformation());
        return menuContent;
    }

    private Component buildVersionInformation() {
        MVerticalLayout layout = new MVerticalLayout().withUndefinedWidth();
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
        settingsItem.addItem("Edit Profile", (MenuBar.Command) selectedItem -> userProfileWindow.open());
        /*settingsItem.addItem("Preferences", new Command() {
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
        Button valoMenuToggleButton = new Button("Menu", (ClickListener) event -> {
            if (getCompositionRoot().getStyleName().contains(STYLE_VISIBLE)) {
                getCompositionRoot().removeStyleName(STYLE_VISIBLE);
            } else {
                getCompositionRoot().addStyleName(STYLE_VISIBLE);
            }
        });
        valoMenuToggleButton.setIcon(VaadinIcons.LIST);
        valoMenuToggleButton.addStyleName("valo-menu-toggle");
        valoMenuToggleButton.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        valoMenuToggleButton.addStyleName(ValoTheme.BUTTON_SMALL);
        return valoMenuToggleButton;
    }

    private Component buildMenuItems() {
        CssLayout menuItemsLayout = new CssLayout();
        menuItemsLayout.addStyleName("valo-menuitems");

        for (MainMenuEntry mainMenuEntry : MainMenuEntry.values()) {
            MenuItemButton menuItemButton = new MenuItemButton(mainMenuEntry);
            menuItemsLayout.addComponent(menuItemButton);
        }

        return menuItemsLayout;
    }

    private void updateUserName() {
        User user = getCurrentUser();
        settingsItem.setText(StringUtils.stripToEmpty(user.getFirstName()) + " " + StringUtils.stripToEmpty(user.getLastName()));
    }
}
