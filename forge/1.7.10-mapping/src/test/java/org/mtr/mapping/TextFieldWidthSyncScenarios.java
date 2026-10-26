package org.mtr.mapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import org.mtr.mapping.mapper.TextFieldWidgetExtension;
import org.mtr.mapping.tool.TextCase;

/** Real mapper and GuiTextField; no draw, Minecraft singleton or graphics constructors. */
public final class TextFieldWidthSyncScenarios {
    private static final class MetricsFont extends FontRenderer {
        private MetricsFont() { super(null,null,null,false); }
        @Override public int getCharWidth(char value) { return value==167?-1:6; }
    }
    private static final Object UNSAFE;
    private static final Method ALLOCATE;
    static {
        try {
            Class<?> type=Class.forName("sun.misc.Unsafe");Field field=type.getDeclaredField("theUnsafe");field.setAccessible(true);
            UNSAFE=field.get(null);ALLOCATE=type.getMethod("allocateInstance",Class.class);
        }catch(Exception problem){throw new ExceptionInInitializerError(problem);}
    }
    private static <T> T allocate(Class<T> type)throws Exception { return type.cast(ALLOCATE.invoke(UNSAFE,type)); }
    private static void check(boolean value,String message) { if(!value)throw new AssertionError(message); }
    private static void put(TextFieldWidgetExtension widget,String name,Object value)throws Exception {
        Field field=TextFieldWidgetExtension.class.getDeclaredField(name);field.setAccessible(true);field.set(widget,value);
    }
    private static GuiTextField nativeField(TextFieldWidgetExtension widget)throws Exception {
        Field field=TextFieldWidgetExtension.class.getDeclaredField("field");field.setAccessible(true);return (GuiTextField)field.get(widget);
    }
    private static int scroll(GuiTextField field)throws Exception {
        Field offset=GuiTextField.class.getDeclaredField("lineScrollOffset");offset.setAccessible(true);return offset.getInt(field);
    }
    private static void sync(TextFieldWidgetExtension widget)throws Exception {
        Method method=TextFieldWidgetExtension.class.getDeclaredMethod("sync");method.setAccessible(true);method.invoke(widget);
    }
    private static TextFieldWidgetExtension widget(FontRenderer font)throws Exception {
        TextFieldWidgetExtension widget=allocate(TextFieldWidgetExtension.class);
        GuiTextField field=new GuiTextField(font,0,0,0,20);field.setMaxStringLength(Integer.MAX_VALUE);
        put(widget,"field",field);put(widget,"maxLength",1024);put(widget,"textCase",TextCase.DEFAULT);
        put(widget,"listener",(Consumer<String>)value -> {});
        widget.visible=true;widget.active=true;widget.setHeight(20);widget.setWidth2(95);
        return widget;
    }
    private static void reopenedNameSurvivesFirstLayout(FontRenderer font)throws Exception {
        TextFieldWidgetExtension widget=widget(font);GuiTextField field=nativeField(widget);
        widget.setText2("Порт GTNH");
        check(field.width==0&&scroll(field)==9,"fixture must retain hidden native width0 and end scroll before first render");
        sync(widget);
        String visible=font.trimStringToWidth(field.getText().substring(scroll(field)),field.getWidth());
        check("Порт GTNH".equals(visible),"reopened field text invisible after first layout; scroll="+scroll(field)+" visible="+visible);
        check("Порт GTNH".equals(widget.getText2())&&field.getCursorPosition()==9&&field.getSelectionEnd()==9,
                "first layout changed text/cursor/selection");
    }
    private static void resizePreservesCursorAndSelection(FontRenderer font)throws Exception {
        for(int[] endpoints:new int[][]{{3,8},{8,3}}) {
            TextFieldWidgetExtension widget=widget(font);GuiTextField field=nativeField(widget);sync(widget);
            widget.setText2("01234567890123456789");field.setCursorPosition(endpoints[0]);field.setSelectionPos(endpoints[1]);
            for(int width:new int[]{20,200}) {
                widget.setWidth2(width);sync(widget);
                check(field.getCursorPosition()==endpoints[0]&&field.getSelectionEnd()==endpoints[1],"resize moved cursor or selection endpoint");
                check("34567".equals(field.getSelectedText())&&"01234567890123456789".equals(widget.getText2()),"resize changed selected text or field contents");
                String visible=font.trimStringToWidth(field.getText().substring(scroll(field)),field.getWidth());
                check(!visible.isEmpty(),"resize left a fully blank viewport");
                int offset=scroll(field);sync(widget);check(scroll(field)==offset,"unchanged width moved the viewport");
            }
        }
    }
    public void textFieldFirstLayoutAndResizePreserveTextCursorAndSelection()throws Exception {
        FontRenderer font=allocate(MetricsFont.class);
        reopenedNameSurvivesFirstLayout(font);resizePreservesCursorAndSelection(font);
    }
}
