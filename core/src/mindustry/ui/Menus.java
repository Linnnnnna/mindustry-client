package mindustry.ui;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.client.*;
import mindustry.client.utils.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

/** Class for handling menus and notifications across the network. Unstable API! */
public class Menus{
    private static final Seq<MenuListener> menuListeners = new Seq<>();

    /** Register a *global* menu listener. If no option is chosen, the option is returned as -1. */
    public static int registerMenu(MenuListener listener){
        menuListeners.add(listener);
        return menuListeners.size - 1;
    }

    //do not invoke any of the methods below directly, use Call

    @Remote(variants = Variant.both)
    public static void menu(int menuId, String title, String message, String[][] options){
        if(title == null) title = "";
        if(options == null) options = new String[0][0];
        if(options[0][0].contains("") && options[0][1].contains("")) return; // .io is annoying
        if(title.contains("Rate this map!") && options[0][0].contains("Yes") && options[0][1].contains("No") && ClientUtils.phoenix()) return; // phoenix network is equally annoying

        Log.debug("Displaying menu " + menuId + " with title: " + title);
        ui.showMenu(title, message, options, (option) -> Call.menuChoose(player, menuId, option));
    }

    @Remote(targets = Loc.both, called = Loc.both)
    public static void menuChoose(@Nullable Player player, int menuId, int option){
        if(player != null){
            Events.fire(new MenuOptionChooseEvent(player, menuId, option));
            if(menuId >= 0 && menuId < menuListeners.size){
                menuListeners.get(menuId).get(player, option);
            }
        }
    }

    @Remote(variants = Variant.both, unreliable = true)
    public static void setHudText(String message){
        if(message == null) return;

        ui.hudfrag.setHudText(message);
    }

    @Remote(variants = Variant.both)
    public static void hideHudText(){
        ui.hudfrag.toggleHudText(false);
    }

    /** TCP version */
    @Remote(variants = Variant.both)
    public static void setHudTextReliable(String message){
        setHudText(message);
    }

    @Remote(variants = Variant.both)
    public static void announce(String message){
        if(message == null) return;

        ui.announce(message);
    }

    @Remote(variants = Variant.both)
    public static void infoMessage(String message){
        if(message == null) return;
        if((ClientUtils.io() || ClientUtils.phoenix()) && Time.timeSinceMillis(ClientVars.lastJoinTime) < 1000) return;

        ui.showText("", message);
    }

    @Remote(variants = Variant.both, unreliable = true)
    public static void infoPopup(String message, float duration, int align, int top, int left, int bottom, int right){
        if(message == null) return;

        ui.showInfoPopup(message, duration, align, top, left, bottom, right);
    }

    @Remote(variants = Variant.both, unreliable = true)
    public static void label(String message, float duration, float worldx, float worldy){
        if(message == null) return;

        ui.showLabel(message, duration, worldx, worldy);
    }

    @Remote(variants = Variant.both)
    public static void infoPopupReliable(String message, float duration, int align, int top, int left, int bottom, int right){
        if(message == null) return;

        ui.showInfoPopup(message, duration, align, top, left, bottom, right);
    }

    @Remote(variants = Variant.both)
    public static void labelReliable(String message, float duration, float worldx, float worldy){
        label(message, duration, worldx, worldy);
    }

    @Remote(variants = Variant.both)
    public static void infoToast(String message, float duration){
        if(message == null) return;
        if(!Core.settings.getBool("showtoasts")) return; // .io admins are known to abuse this
        ui.showInfoToast(message, duration);
    }

    @Remote(variants = Variant.both)
    public static void warningToast(int unicode, String text){
        if(text == null || Fonts.icon.getData().getGlyph((char)unicode) == null) return;
        if(!Core.settings.getBool("showtoasts")) return;

        ui.hudfrag.showToast(Fonts.getGlyph(Fonts.icon, (char)unicode), text);
    }

    @Remote(variants = Variant.both)
    public static void openURI(String uri){
        if(uri == null) return;

        ui.showConfirm(Core.bundle.format("linkopen", uri), () -> Core.app.openURI(uri));
    }

    //internal use only
    @Remote
    public static void removeWorldLabel(int id){
        var label = Groups.label.getByID(id);
        if(label != null){
            label.remove();
        }
    }

    public interface MenuListener{
        void get(Player player, int option);
    }
}
