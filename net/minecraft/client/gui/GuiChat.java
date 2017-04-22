package net.minecraft.client.gui;

import java.io.IOException;
import java.util.HashMap;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ITabCompleter;
import net.minecraft.util.TabCompleter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import nl.lucemans.shape.Shape;
import nl.lucemans.shape.utils.ChatUtils;

public class GuiChat extends GuiScreen implements ITabCompleter
{
    private static final Logger LOGGER = LogManager.getLogger();
    private String historyBuffer = "";
    private HashMap<String, Integer> colors = new HashMap<String, Integer>();
    
    /**
     * keeps position of which chat message you will select when you press up, (does not increase for duplicated
     * messages sent immediately after each other)
     */
    private int sentHistoryCursor = -1;
    private TabCompleter tabCompleter;

    /** Chat entry field */
    protected GuiTextField inputField;

    /**
     * is the text that appears when you press the chat key and the input box appears pre-filled
     */
    private String defaultInputFieldText = "";

    public GuiChat()
    {
    }

    public GuiChat(String defaultText)
    {
        this.defaultInputFieldText = defaultText;
    }

    /**
     * Adds the buttons (and other controls) to the screen in question. Called when the GUI is displayed and when the
     * window resizes, the buttonList is cleared beforehand.
     */
    public void initGui()
    {
        Keyboard.enableRepeatEvents(true);
        this.sentHistoryCursor = this.mc.ingameGUI.getChatGUI().getSentMessages().size();
        this.inputField = new GuiTextField(0, this.fontRendererObj, 4, this.height - 12, this.width - 4, 12);
        this.inputField.setMaxStringLength(256);
        this.inputField.setEnableBackgroundDrawing(false);
        this.inputField.setFocused(true);
        this.inputField.setText(this.defaultInputFieldText);
        this.inputField.setCanLoseFocus(false);
        this.tabCompleter = new GuiChat.ChatTabCompleter(this.inputField);
        colors.clear();
        colors.put("&4", 0xeeBE0000);
        colors.put("&c", 0xeeFE3F3F);
        colors.put("&6", 0xeeD9A334);
        colors.put("&e", 0xeeFEFE3F);
        colors.put("&2", 0xee00BE00);
        colors.put("&a", 0xee3FFE3F);
        colors.put("&b", 0xee3FFEFE);
        colors.put("&3", 0xee00BEBE);
        colors.put("&1", 0xee0000BE);
        colors.put("&9", 0xee3F3FFE);
        colors.put("&d", 0xeeFE3FFE);
        colors.put("&5", 0xeeBE00BE);
        colors.put("&f", 0xeeFFFFFF);
        colors.put("&7", 0xeeBEBEBE);
        colors.put("&8", 0xee828282);
        colors.put("&0", 0xee000000);
    }

    /**
     * Called when the screen is unloaded. Used to disable keyboard repeat events
     */
    public void onGuiClosed()
    {
        Keyboard.enableRepeatEvents(false);
        this.mc.ingameGUI.getChatGUI().resetScroll();
    }

    /**
     * Called from the main game loop to update the screen.
     */
    public void updateScreen()
    {
        this.inputField.updateCursorCounter();
    }

    /**
     * Fired when a key is typed (except F11 which toggles full screen). This is the equivalent of
     * KeyListener.keyTyped(KeyEvent e). Args : character (character on the key), keyCode (lwjgl Keyboard key code)
     */
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        this.tabCompleter.resetRequested();

        if (keyCode == 15)
        {
            this.tabCompleter.complete();
        }
        else
        {
            this.tabCompleter.resetDidComplete();
        }

        if (keyCode == 1)
        {
            this.mc.displayGuiScreen((GuiScreen)null);
        }
        else if (keyCode != 28 && keyCode != 156)
        {
            if (keyCode == 200)
            {
                this.getSentHistory(-1);
            }
            else if (keyCode == 208)
            {
                this.getSentHistory(1);
            }
            else if (keyCode == 201)
            {
                this.mc.ingameGUI.getChatGUI().scroll(this.mc.ingameGUI.getChatGUI().getLineCount() - 1);
            }
            else if (keyCode == 209)
            {
                this.mc.ingameGUI.getChatGUI().scroll(-this.mc.ingameGUI.getChatGUI().getLineCount() + 1);
            }
            else
            {
                this.inputField.textboxKeyTyped(typedChar, keyCode);
            }
        }
        else
        {
            String s = this.inputField.getText().trim();

            if (s.startsWith("."))
            {
            	String _s = s.replace(".", "");
            	Shape.INSTANCE.ircBot.sendMessage(_s);
            	ChatUtils.irc(Minecraft.getMinecraft().session.getUsername(), _s);
            }
            else
            {
	            if (!s.isEmpty())
	            {
	                this.sendChatMessage(s);
	            }
            }

            this.mc.displayGuiScreen((GuiScreen)null);
        }
    }

    /**
     * Handles mouse input.
     */
    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        int i = Mouse.getEventDWheel();

        if (i != 0)
        {
            if (i > 1)
            {
                i = 1;
            }

            if (i < -1)
            {
                i = -1;
            }

            if (!isShiftKeyDown())
            {
                i *= 7;
            }

            this.mc.ingameGUI.getChatGUI().scroll(i);
        }
    }

    /**
     * Called when the mouse is clicked. Args : mouseX, mouseY, clickedButton
     */
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        if (mouseButton == 0)
        {
            ITextComponent itextcomponent = this.mc.ingameGUI.getChatGUI().getChatComponent(Mouse.getX(), Mouse.getY());

            if (itextcomponent != null && this.handleComponentClick(itextcomponent))
            {
                return;
            }
        }
        
        //TODO: Color Implant
        Integer hor = 0;
        Integer vert = 0;
        for (String str : colors.keySet())
        {
        	Integer start = 4;
        	Integer perRow = 4;
        	Integer wid = (150-(start*2))/perRow+(1/perRow);
        	int code = colors.get(str);
        	Integer _x = width-150 + (hor*(wid))+start;
        	Integer _y = height-170 + + start + (vert*(wid));
        	if (mouseX >= _x
        			&& mouseY >= _y
        			&& mouseX <= _x+wid
        			&& mouseY <= _y+wid)
        	{
        		//this.inputField.text = this.inputField.text.substring(this.inputField.cursorPosition) + str + this.inputField.text.substring(this.inputField.cursorPosition, this.inputField.text.length());
        		//this.inputField.cursorPosition += str.length();
        		this.inputField.writeText(str);
        	}
        	
        	hor += 1;
        	if (hor >= perRow)
        	{
        		hor -= perRow;
        		vert += 1;
        	}
        }

        this.inputField.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    /**
     * Sets the text of the chat
     */
    protected void setText(String newChatText, boolean shouldOverwrite)
    {
        if (shouldOverwrite)
        {
            this.inputField.setText(newChatText);
        }
        else
        {
            this.inputField.writeText(newChatText);
        }
    }

    /**
     * input is relative and is applied directly to the sentHistoryCursor so -1 is the previous message, 1 is the next
     * message from the current cursor position
     */
    public void getSentHistory(int msgPos)
    {
        int i = this.sentHistoryCursor + msgPos;
        int j = this.mc.ingameGUI.getChatGUI().getSentMessages().size();
        i = MathHelper.clamp(i, 0, j);

        if (i != this.sentHistoryCursor)
        {
            if (i == j)
            {
                this.sentHistoryCursor = j;
                this.inputField.setText(this.historyBuffer);
            }
            else
            {
                if (this.sentHistoryCursor == j)
                {
                    this.historyBuffer = this.inputField.getText();
                }

                this.inputField.setText((String)this.mc.ingameGUI.getChatGUI().getSentMessages().get(i));
                this.sentHistoryCursor = i;
            }
        }
    }

    /**
     * Draws the screen and all the components in it.
     */
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        drawRect(2, this.height - 14, this.width - 2, this.height - 2, Integer.MIN_VALUE);
        this.inputField.drawTextBox();
        ITextComponent itextcomponent = this.mc.ingameGUI.getChatGUI().getChatComponent(Mouse.getX(), Mouse.getY());

        if (itextcomponent != null && itextcomponent.getStyle().getHoverEvent() != null)
        {
            this.handleComponentHover(itextcomponent, mouseX, mouseY);
        }
        
        //TODO: Shape Color Chat
        drawRect(width-150, height-170, width, height, 0x33ffffff);
        Integer hor = 0;
        Integer vert = 0;
        for (String str : colors.keySet())
        {
        	Integer start = 4;
        	Integer perRow = 4;
        	Integer wid = (150-(start*2))/perRow+(1/perRow);
        	int code = colors.get(str);
        	Integer _x = width-150 + (hor*(wid))+start;
        	Integer _y = height-170 + + start + (vert*(wid));
        	drawRect(_x+wid/16, _y+wid/16, _x+wid-wid/16, _y+wid-wid/16, code);
        	
        	hor += 1;
        	if (hor >= perRow)
        	{
        		hor -= perRow;
        		vert += 1;
        	}
        }
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /**
     * Returns true if this GUI should pause the game when it is displayed in single-player
     */
    public boolean doesGuiPauseGame()
    {
        return false;
    }

    /**
     * Sets the list of tab completions, as long as they were previously requested.
     */
    public void setCompletions(String... newCompletions)
    {
        this.tabCompleter.setCompletions(newCompletions);
    }

    public static class ChatTabCompleter extends TabCompleter
    {
        private final Minecraft clientInstance = Minecraft.getMinecraft();

        public ChatTabCompleter(GuiTextField p_i46749_1_)
        {
            super(p_i46749_1_, false);
        }

        public void complete()
        {
            super.complete();

            if (this.completions.size() > 1)
            {
                StringBuilder stringbuilder = new StringBuilder();

                for (String s : this.completions)
                {
                    if (stringbuilder.length() > 0)
                    {
                        stringbuilder.append(", ");
                    }

                    stringbuilder.append(s);
                }

                this.clientInstance.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(new TextComponentString(stringbuilder.toString()), 1);
            }
        }

        @Nullable
        public BlockPos getTargetBlockPos()
        {
            BlockPos blockpos = null;

            if (this.clientInstance.objectMouseOver != null && this.clientInstance.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK)
            {
                blockpos = this.clientInstance.objectMouseOver.getBlockPos();
            }

            return blockpos;
        }
    }
}
