package net.minecraft.client.gui;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.glu.Project;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.realms.RealmsBridge;
import net.minecraft.src.CustomPanorama;
import net.minecraft.src.CustomPanoramaProperties;
import net.minecraft.src.Reflector;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.demo.DemoWorldServer;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.WorldInfo;
import nl.lucemans.shape.Shape;
import nl.lucemans.shape.file.Updater;
import nl.lucemans.shape.gui.FriendsGui;
import nl.lucemans.shape.gui.ShapeGuiAdmin;
import nl.lucemans.shape.gui.ShapeGuiAltManager;
import nl.lucemans.shape.gui.ShapeGuiShop;
import nl.lucemans.shape.gui.ShapeGuiTermsAndAgreements;
import nl.lucemans.shape.gui.ShapeGuiThemeSelector;
import nl.lucemans.shape.irc.IrcBot;

public class GuiMainMenu extends GuiScreen
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random RANDOM = new Random();

    /** Counts the number of screen updates. */
    private final float updateCounter;

    /** The splash message. */
    private String splashText;
    private GuiButton buttonResetDemo;

    /** Timer used to rotate the panorama, increases every tick. */
    private int panoramaTimer;

    /**
     * Texture allocated for the current viewport of the main menu's panorama background.
     */
    private DynamicTexture viewportTexture;

    /**
     * The Object object utilized as a thread lock when performing non thread-safe operations
     */
    private final Object threadLock = new Object();
    public static final String MORE_INFO_TEXT = "Please click " + TextFormatting.UNDERLINE + "here" + TextFormatting.RESET + " for more information.";

    /** Width of openGLWarning2 */
    private int openGLWarning2Width;

    /** Width of openGLWarning1 */
    private int openGLWarning1Width;

    /** Left x coordinate of the OpenGL warning */
    private int openGLWarningX1;

    /** Top y coordinate of the OpenGL warning */
    private int openGLWarningY1;

    /** Right x coordinate of the OpenGL warning */
    private int openGLWarningX2;

    /** Bottom y coordinate of the OpenGL warning */
    private int openGLWarningY2;

    /** OpenGL graphics card warning. */
    private String openGLWarning1;

    /** OpenGL graphics card warning. */
    private String openGLWarning2;

    /** Link to the Mojang Support about minimum requirements */
    private String openGLWarningLink;
    private static final ResourceLocation SPLASH_TEXTS = new ResourceLocation("texts/splashes.txt");
    private static final ResourceLocation MINECRAFT_TITLE_TEXTURES = new ResourceLocation("textures/gui/title/minecraft.png");

    /** An array of all the paths to the panorama pictures. */
    private static final ResourceLocation[] TITLE_PANORAMA_PATHS = new ResourceLocation[] {new ResourceLocation("textures/gui/title/background/panorama_0.png"), new ResourceLocation("textures/gui/title/background/panorama_1.png"), new ResourceLocation("textures/gui/title/background/panorama_2.png"), new ResourceLocation("textures/gui/title/background/panorama_3.png"), new ResourceLocation("textures/gui/title/background/panorama_4.png"), new ResourceLocation("textures/gui/title/background/panorama_5.png")};
    private ResourceLocation backgroundTexture;

    /** Minecraft Realms button. */
    private GuiButton realmsButton;

    /** Has the check for a realms notification screen been performed? */
    private boolean hasCheckedForRealmsNotification;

    /**
     * A screen generated by realms for notifications; drawn in adition to the main menu (buttons and such from both are
     * drawn at the same time). May be null.
     */
    private GuiScreen realmsNotification;
    private GuiButton modButton;
    private GuiScreen modUpdateNotification;

    //TODO: INIT VARIABLED
    private String shapeinput = " > ";
    
    public GuiMainMenu()
    {
        this.openGLWarning2 = MORE_INFO_TEXT;
        this.splashText = "missingno";
        IResource iresource = null;

        try
        {
            List<String> list = Lists.<String>newArrayList();
            iresource = Minecraft.getMinecraft().getResourceManager().getResource(SPLASH_TEXTS);
            BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(iresource.getInputStream(), Charsets.UTF_8));
            String s;

            while ((s = bufferedreader.readLine()) != null)
            {
                s = s.trim();

                if (!s.isEmpty())
                {
                    list.add(s);
                }
            }

            if (!list.isEmpty())
            {
                while (true)
                {
                    this.splashText = (String)list.get(RANDOM.nextInt(list.size()));

                    if (this.splashText.hashCode() != 125780783)
                    {
                        break;
                    }
                }
            }
        }
        catch (IOException var8)
        {
            ;
        }
        finally
        {
            IOUtils.closeQuietly((Closeable)iresource);
        }

        this.updateCounter = RANDOM.nextFloat();
        this.openGLWarning1 = "";

        if (!GLContext.getCapabilities().OpenGL20 && !OpenGlHelper.areShadersSupported())
        {
            this.openGLWarning1 = I18n.format("title.oldgl1", new Object[0]);
            this.openGLWarning2 = I18n.format("title.oldgl2", new Object[0]);
            this.openGLWarningLink = "https://help.mojang.com/customer/portal/articles/325948?ref=game";
        }

        String s1 = System.getProperty("java.version");

        if (s1 != null && (s1.startsWith("1.6") || s1.startsWith("1.7")))
        {
            this.openGLWarning1 = I18n.format("title.oldjava1", new Object[0]);
            this.openGLWarning2 = I18n.format("title.oldjava2", new Object[0]);
            this.openGLWarningLink = "https://help.mojang.com/customer/portal/articles/2636196?ref=game";
        }
    }

    /**
     * Is there currently a realms notification screen, and are realms notifications enabled?
     */
    private boolean areRealmsNotificationsEnabled()
    {
        return Minecraft.getMinecraft().gameSettings.getOptionOrdinalValue(GameSettings.Options.REALMS_NOTIFICATIONS) && this.realmsNotification != null;
    }

    /**
     * Called from the main game loop to update the screen.
     */
    public void updateScreen()
    {
        ++this.panoramaTimer;

        if (this.areRealmsNotificationsEnabled())
        {
            this.realmsNotification.updateScreen();
        }
    }

    /**
     * Returns true if this GUI should pause the game when it is displayed in single-player
     */
    public boolean doesGuiPauseGame()
    {
        return false;
    }

    /**
     * Fired when a key is typed (except F11 which toggles full screen). This is the equivalent of
     * KeyListener.keyTyped(KeyEvent e). Args : character (character on the key), keyCode (lwjgl Keyboard key code)
     */
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
    	//TODO: STORES NOTE
    	if (Character.isLetterOrDigit(typedChar))
    	{
    		this.shapeinput += typedChar; //Keyboard.getKeyName(keyCode);
    	}
    	else
    	{
    		// not alphanumerical
    		if (Keyboard.getKeyName(keyCode).equalsIgnoreCase("SPACE"))
    		{
    			this.shapeinput += " ";
    		}
    		else if (Keyboard.getKeyName(keyCode).equalsIgnoreCase("BACK"))
    		{
    			if (this.shapeinput.length() > 3)
    			{
    				this.shapeinput = this.shapeinput.substring(0, this.shapeinput.length()-1);
    			}
    		}
    		else if (Keyboard.getKeyName(keyCode).equalsIgnoreCase("RETURN"))
    		{
    			this.shapeinput += "\n";
    		}
    		else
    		{
    			//this.shapeinput += "?"; //Keyboard.getKeyName(keyCode);
    		}
    	}
    }

    /**
     * Adds the buttons (and other controls) to the screen in question. Called when the GUI is displayed and when the
     * window resizes, the buttonList is cleared beforehand.
     */
    public void initGui()
    {
		if (!Shape.INSTANCE.props.isTermsAgreed())
			mc.displayGuiScreen(new ShapeGuiTermsAndAgreements(this)); System.out.println("User has not agreed to our Terms");
    	
        this.viewportTexture = new DynamicTexture(256, 256);
        this.backgroundTexture = this.mc.getTextureManager().getDynamicTextureLocation("background", this.viewportTexture);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        if (calendar.get(2) + 1 == 12 && calendar.get(5) == 24)
        {
            this.splashText = "Merry X-mas!";
        }
        else if (calendar.get(2) + 1 == 1 && calendar.get(5) == 1)
        {
            this.splashText = "Happy new year!";
        }
        else if (calendar.get(2) + 1 == 10 && calendar.get(5) == 31)
        {
            this.splashText = "OOoooOOOoooo! Spooky!";
        }
        else
        {
        	this.splashText = Shape.resolveSplash(this.splashText, calendar);
        }

        int i = 24;
        int j = this.height / 4 + 48;

        if (this.mc.isDemo())
        {
            this.addDemoButtons(j, 24);
        }
        else
        {
            this.addSingleplayerMultiplayerButtons(j, 24);
        }

        //this.buttonList.add(new GuiButton(0, this.width / 2 - 100, j + 72 + 12, 98, 20, I18n.format("menu.options", new Object[0])));
        //this.buttonList.add(new GuiButton(4, this.width / 2 + 2, j + 72 + 12, 98, 20, I18n.format("menu.quit", new Object[0])));
        this.buttonList.add(new GuiButton(0, this.width / 2 - 100, j + 72 + 12, 200/5-3, 20, "Options")); // language = 5
        this.buttonList.add(new GuiButton(21, this.width / 2 - 100 + 200/5 + 1, j + 72 + 12, 200/5-3, 20, "Web"));
        this.buttonList.add(new GuiButton(20, this.width / 2 - 100 + 200/5 + 200/5 + 2, j + 72 + 12, 200/5-3, 20, "Stats"));
        this.buttonList.add(new GuiButton(23, this.width / 2 - 100 + 200/5 + 200/5 + 200/5 + 3, j + 72 + 12, 200/5-3, 20, "Update"));
        this.buttonList.add(new GuiButton(4, this.width / 2 - 100 + 200/5 + 200/5 + 200/5 + 200/5 + 4, j + 72 + 12, 200/5-3, 20, "Exit"));
        this.buttonList.add(new GuiButton(24, 0,0,"Themes"));
        this.buttonList.add(new GuiButton(25, 0, 20, "Admin"));
        this.buttonList.add(new GuiButton(26, 0, 40, "Shop"));
        //TODO: 
        //this.buttonList.add(new GuiButtonWebsite(20, this.width / 2 - 100 + 20 + 4, j + 72 + 12 + 24));

        synchronized (this.threadLock)
        {
            this.openGLWarning1Width = this.fontRendererObj.getStringWidth(this.openGLWarning1);
            this.openGLWarning2Width = this.fontRendererObj.getStringWidth(this.openGLWarning2);
            int k = Math.max(this.openGLWarning1Width, this.openGLWarning2Width);
            this.openGLWarningX1 = (this.width - k) / 2;
            this.openGLWarningY1 = ((GuiButton)this.buttonList.get(0)).yPosition - 24;
            this.openGLWarningX2 = this.openGLWarningX1 + k;
            this.openGLWarningY2 = this.openGLWarningY1 + 24;
        }

        this.mc.setConnectedToRealms(false);

        if (Minecraft.getMinecraft().gameSettings.getOptionOrdinalValue(GameSettings.Options.REALMS_NOTIFICATIONS) && !this.hasCheckedForRealmsNotification)
        {
            RealmsBridge realmsbridge = new RealmsBridge();
            this.realmsNotification = realmsbridge.getNotificationScreen(this);
            this.hasCheckedForRealmsNotification = true;
        }

        if (this.areRealmsNotificationsEnabled())
        {
            this.realmsNotification.setGuiSize(this.width, this.height);
            this.realmsNotification.initGui();
        }

        if (Reflector.NotificationModUpdateScreen_init.exists())
        {
            this.modUpdateNotification = (GuiScreen)Reflector.call(Reflector.NotificationModUpdateScreen_init, new Object[] {this, this.modButton});
        }
    }

    /**
     * Adds Singleplayer and Multiplayer buttons on Main Menu for players who have bought the game.
     */
    private void addSingleplayerMultiplayerButtons(int p_73969_1_, int p_73969_2_)
    {
        this.buttonList.add(new GuiButton(1, this.width / 2 - 100, p_73969_1_, I18n.format("menu.singleplayer", new Object[0])));
        this.buttonList.add(new GuiButton(2, this.width / 2 - 100, p_73969_1_ + p_73969_2_ * 1, I18n.format("menu.multiplayer", new Object[0])));

        if (Reflector.GuiModList_Constructor.exists())
        {
            this.realmsButton = this.addButton(new GuiButton(14, this.width / 2 + 2, p_73969_1_ + p_73969_2_ * 2, 98, 20, I18n.format("menu.online", new Object[0]).replace("Minecraft", "").trim()));
            this.buttonList.add(this.modButton = new GuiButton(6, this.width / 2 - 100, p_73969_1_ + p_73969_2_ * 2, 98, 20, I18n.format("fml.menu.mods", new Object[0])));
        }
        else
        {
            this.realmsButton = this.addButton(new GuiButton(14, this.width / 2 - 100, p_73969_1_ + p_73969_2_ * 2, 96, 20, I18n.format("menu.online", new Object[0])));
            this.buttonList.add(new GuiButton(22, this.width / 2 + 100 - 96, p_73969_1_ + p_73969_2_ * 2, 96, 20, "Alt Manager"));
        }
    }

    /**
     * Adds Demo buttons on Main Menu for players who are playing Demo.
     */
    private void addDemoButtons(int p_73972_1_, int p_73972_2_)
    {
        this.buttonList.add(new GuiButton(11, this.width / 2 - 100, p_73972_1_, I18n.format("menu.playdemo", new Object[0])));
        this.buttonResetDemo = this.addButton(new GuiButton(12, this.width / 2 - 100, p_73972_1_ + p_73972_2_ * 1, I18n.format("menu.resetdemo", new Object[0])));
        ISaveFormat isaveformat = this.mc.getSaveLoader();
        WorldInfo worldinfo = isaveformat.getWorldInfo("Demo_World");

        if (worldinfo == null)
        {
            this.buttonResetDemo.enabled = false;
        }
    }

    /**
     * Called by the controls from the buttonList when activated. (Mouse pressed for buttons)
     */
    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (button.id == 0)
        {
            this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings));
        }

        if (button.id == 5)
        {
            this.mc.displayGuiScreen(new GuiLanguage(this, this.mc.gameSettings, this.mc.getLanguageManager()));
        }

        if (button.id == 1)
        {
            this.mc.displayGuiScreen(new GuiWorldSelection(this));
        }

        if (button.id == 2)
        {
            this.mc.displayGuiScreen(new GuiMultiplayer(this));
        }

        if (button.id == 14 && this.realmsButton.visible)
        {
            this.switchToRealms();
        }

        if (button.id == 4)
        {
            this.mc.shutdown();
        }

        if (button.id == 6 && Reflector.GuiModList_Constructor.exists())
        {
            this.mc.displayGuiScreen((GuiScreen)Reflector.newInstance(Reflector.GuiModList_Constructor, new Object[] {this}));
        }

        if (button.id == 11)
        {
            this.mc.launchIntegratedServer("Demo_World", "Demo_World", DemoWorldServer.DEMO_WORLD_SETTINGS);
        }

        if (button.id == 12)
        {
            ISaveFormat isaveformat = this.mc.getSaveLoader();
            WorldInfo worldinfo = isaveformat.getWorldInfo("Demo_World");

            if (worldinfo != null)
            {
                this.mc.displayGuiScreen(new GuiYesNo(this, I18n.format("selectWorld.deleteQuestion", new Object[0]), "\'" + worldinfo.getWorldName() + "\' " + I18n.format("selectWorld.deleteWarning", new Object[0]), I18n.format("selectWorld.deleteButton", new Object[0]), I18n.format("gui.cancel", new Object[0]), 12));
            }
        }
        //TODO: Client Buttons
        if (button.id == 20)
        {
            this.mc.displayGuiScreen(new FriendsGui(this));
        	//
        	//Shape.INSTANCE.ircBot.openConnection("#shapeclientmain");
        }
        if (button.id == 21)
        {
        	//TODO: MAKE OPEN WEBPAGE SYSTEM
        	System.out.println("OPEN A WEBPAGE");
        	try {
        	this.openWebLink(new URI("http://lucemans.nl"));
        	}catch(Exception e)
        	{
        		e.printStackTrace();
        	}
        }
        if (button.id == 22)
        {
        	this.mc.displayGuiScreen(new ShapeGuiAltManager(this));
        }
        if (button.id == 23)
        {
        	Updater.CheckForUpdates();
        }
        if (button.id == 24)
        {
        	this.mc.displayGuiScreen(new ShapeGuiThemeSelector());
        }
        if (button.id == 25)
        {
        	this.mc.displayGuiScreen(new ShapeGuiAdmin(this));
        }
        if (button.id == 26)
        {
        	this.mc.displayGuiScreen(new ShapeGuiShop());
        }
    }

    private void switchToRealms()
    {
        RealmsBridge realmsbridge = new RealmsBridge();
        realmsbridge.switchToRealms(this);
    }

    public void confirmClicked(boolean result, int id)
    {
        if (result && id == 12)
        {
            ISaveFormat isaveformat = this.mc.getSaveLoader();
            isaveformat.flushCache();
            isaveformat.deleteWorldDirectory("Demo_World");
            this.mc.displayGuiScreen(this);
        }
        else if (id == 12)
        {
            this.mc.displayGuiScreen(this);
        }
        else if (id == 13)
        {
            if (result)
            {
                try
                {
                    Class<?> oclass = Class.forName("java.awt.Desktop");
                    Object object = oclass.getMethod("getDesktop", new Class[0]).invoke((Object)null, new Object[0]);
                    oclass.getMethod("browse", new Class[] {URI.class}).invoke(object, new Object[] {new URI(this.openGLWarningLink)});
                }
                catch (Throwable throwable)
                {
                    LOGGER.error("Couldn\'t open link", throwable);
                }
            }

            this.mc.displayGuiScreen(this);
        }
    }

    /**
     * Draws the main menu panorama
     */
    private void drawPanorama(int mouseX, int mouseY, float partialTicks)
    {
        Tessellator tessellator = Tessellator.getInstance();
        VertexBuffer vertexbuffer = tessellator.getBuffer();
        GlStateManager.matrixMode(5889);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        Project.gluPerspective(120.0F, 1.0F, 0.05F, 10.0F);
        GlStateManager.matrixMode(5888);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        int i = 8;
        int j = 64;
        CustomPanoramaProperties custompanoramaproperties = CustomPanorama.getCustomPanoramaProperties();

        if (custompanoramaproperties != null)
        {
            j = custompanoramaproperties.getBlur1();
        }

        for (int k = 0; k < j; ++k)
        {
            GlStateManager.pushMatrix();
            float f = ((float)(k % 8) / 8.0F - 0.5F) / 64.0F;
            float f1 = ((float)(k / 8) / 8.0F - 0.5F) / 64.0F;
            float f2 = 0.0F;
            GlStateManager.translate(f, f1, 0.0F);
            GlStateManager.rotate(MathHelper.sin(((float)this.panoramaTimer + partialTicks) / 400.0F) * 25.0F + 20.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(-((float)this.panoramaTimer + partialTicks) * 0.1F, 0.0F, 1.0F, 0.0F);

            for (int l = 0; l < 6; ++l)
            {
                GlStateManager.pushMatrix();

                if (l == 1)
                {
                    GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
                }

                if (l == 2)
                {
                    GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                }

                if (l == 3)
                {
                    GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
                }

                if (l == 4)
                {
                    GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                }

                if (l == 5)
                {
                    GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
                }

                ResourceLocation[] aresourcelocation = TITLE_PANORAMA_PATHS;

                if (custompanoramaproperties != null)
                {
                    aresourcelocation = custompanoramaproperties.getPanoramaLocations();
                }

                this.mc.getTextureManager().bindTexture(aresourcelocation[l]);
                vertexbuffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                int i1 = 255 / (k + 1);
                float f3 = 0.0F;
                vertexbuffer.pos(-1.0D, -1.0D, 1.0D).tex(0.0D, 0.0D).color(255, 255, 255, i1).endVertex();
                vertexbuffer.pos(1.0D, -1.0D, 1.0D).tex(1.0D, 0.0D).color(255, 255, 255, i1).endVertex();
                vertexbuffer.pos(1.0D, 1.0D, 1.0D).tex(1.0D, 1.0D).color(255, 255, 255, i1).endVertex();
                vertexbuffer.pos(-1.0D, 1.0D, 1.0D).tex(0.0D, 1.0D).color(255, 255, 255, i1).endVertex();
                tessellator.draw();
                GlStateManager.popMatrix();
            }

            GlStateManager.popMatrix();
            GlStateManager.colorMask(true, true, true, false);
        }

        vertexbuffer.setTranslation(0.0D, 0.0D, 0.0D);
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.matrixMode(5889);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(5888);
        GlStateManager.popMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
    }

    /**
     * Rotate and blurs the skybox view in the main menu
     */
    private void rotateAndBlurSkybox(float partialTicks)
    {
        this.mc.getTextureManager().bindTexture(this.backgroundTexture);
        GlStateManager.glTexParameteri(3553, 10241, 9729);
        GlStateManager.glTexParameteri(3553, 10240, 9729);
        GlStateManager.glCopyTexSubImage2D(3553, 0, 0, 0, 0, 0, 256, 256);
        GlStateManager.enableBlend();
        //TODO: Error Commented Out
        //GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.colorMask(true, true, true, false);
        Tessellator tessellator = Tessellator.getInstance();
        VertexBuffer vertexbuffer = tessellator.getBuffer();
        vertexbuffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        GlStateManager.disableAlpha();
        int i = 3;
        int j = 3;
        CustomPanoramaProperties custompanoramaproperties = CustomPanorama.getCustomPanoramaProperties();

        if (custompanoramaproperties != null)
        {
            j = custompanoramaproperties.getBlur2();
        }

        for (int k = 0; k < j; ++k)
        {
            float f = 1.0F / (float)(k + 1);
            int l = this.width;
            int i1 = this.height;
            float f1 = (float)(k - 1) / 256.0F;
            vertexbuffer.pos((double)l, (double)i1, (double)this.zLevel).tex((double)(0.0F + f1), 1.0D).color(1.0F, 1.0F, 1.0F, f).endVertex();
            vertexbuffer.pos((double)l, 0.0D, (double)this.zLevel).tex((double)(1.0F + f1), 1.0D).color(1.0F, 1.0F, 1.0F, f).endVertex();
            vertexbuffer.pos(0.0D, 0.0D, (double)this.zLevel).tex((double)(1.0F + f1), 0.0D).color(1.0F, 1.0F, 1.0F, f).endVertex();
            vertexbuffer.pos(0.0D, (double)i1, (double)this.zLevel).tex((double)(0.0F + f1), 0.0D).color(1.0F, 1.0F, 1.0F, f).endVertex();
        }

        tessellator.draw();
        GlStateManager.enableAlpha();
        GlStateManager.colorMask(true, true, true, true);
    }

    /**
     * Renders the skybox in the main menu
     */
    private void renderSkybox(int mouseX, int mouseY, float partialTicks)
    {
        this.mc.getFramebuffer().unbindFramebuffer();
        GlStateManager.viewport(0, 0, 256, 256);
        this.drawPanorama(mouseX, mouseY, partialTicks);
        this.rotateAndBlurSkybox(partialTicks);
        int i = 3;
        CustomPanoramaProperties custompanoramaproperties = CustomPanorama.getCustomPanoramaProperties();

        if (custompanoramaproperties != null)
        {
            i = custompanoramaproperties.getBlur3();
        }

        for (int j = 0; j < i; ++j)
        {
            this.rotateAndBlurSkybox(partialTicks);
            this.rotateAndBlurSkybox(partialTicks);
        }

        this.mc.getFramebuffer().bindFramebuffer(true);
        GlStateManager.viewport(0, 0, this.mc.displayWidth, this.mc.displayHeight);
        float f2 = 120.0F / (float)(this.width > this.height ? this.width : this.height);
        float f = (float)this.height * f2 / 256.0F;
        float f1 = (float)this.width * f2 / 256.0F;
        int k = this.width;
        int l = this.height;
        Tessellator tessellator = Tessellator.getInstance();
        VertexBuffer vertexbuffer = tessellator.getBuffer();
        vertexbuffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        vertexbuffer.pos(0.0D, (double)l, (double)this.zLevel).tex((double)(0.5F - f), (double)(0.5F + f1)).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        vertexbuffer.pos((double)k, (double)l, (double)this.zLevel).tex((double)(0.5F - f), (double)(0.5F - f1)).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        vertexbuffer.pos((double)k, 0.0D, (double)this.zLevel).tex((double)(0.5F + f), (double)(0.5F - f1)).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        vertexbuffer.pos(0.0D, 0.0D, (double)this.zLevel).tex((double)(0.5F + f), (double)(0.5F + f1)).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        tessellator.draw();
    }

    /**
     * Draws the screen and all the components in it.
     */
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
    	for(GuiButton button : buttonList)
    	{
    		button.visible = Shape.INSTANCE.themeManager.getTheme().showButtons;
    		if (button.id >= 24 && button.id <= 26)
    			button.visible = true;
    	}
    	
    	if (Shape.INSTANCE.themeManager.getTheme().backgroundImage == null)
    	{
    		drawGradientRect(0, 0, width, height, Shape.INSTANCE.themeManager.getTheme().backgroundGradTop, Shape.INSTANCE.themeManager.getTheme().backgroundGradDown);
    	}
    	else
    	{
    		if (Shape.INSTANCE.XPbg != null)
    		{
    			Minecraft.getMinecraft().getTextureManager()
    			.bindTexture(Shape.INSTANCE.XPbg);
            	glEnable(GL_BLEND);
            	glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            	
            	Integer h = (int) Math.round(((float)height/25.2142857143));
            	
            	drawModalRectWithCustomSizedTexture(0, 0, 0, 0, 1920, 1200, width, height);
            	
            	//System.out.println("HEIGHT: " + height);
            	
            	glDisable(GL_BLEND);
    		}
    		else
    		{
    			// draw background if thing not loads
        		drawGradientRect(0, 0, width, height, Shape.INSTANCE.themeManager.getTheme().backgroundGradTop, Shape.INSTANCE.themeManager.getTheme().backgroundGradDown);
    		}
    	}
    	
    	GL11.glPushMatrix();
    	
    	GL11.glScalef(5f, 5f, 1f);
    	
    	drawCenteredString(mc.fontRendererObj, Shape.INSTANCE.settings.menuTitle, width/5/2, 10, 0xffffffff); //53
    	//drawCenteredString(new FontRenderer(mc.gameSettings, new ResourceLocation("textures/font/asciij.png"), mc.getTextureManager(), false), Shape.INSTANCE.settings.menuTitle, width/5/2, 10, 0xffffffff);
    	
    	GL11.glPopMatrix();
    	
    	//
        GlStateManager.disableAlpha();
        //this.renderSkybox(mouseX, mouseY, partialTicks);
        GlStateManager.enableAlpha();
        int i = 274;
        int j = this.width / 2 - 137;
        int k = 30;
        int l = -2130706433;
        int i1 = 16777215;
        int j1 = 0;
        int k1 = Integer.MIN_VALUE;
        /*
        CustomPanoramaProperties custompanoramaproperties = CustomPanorama.getCustomPanoramaProperties();W

        if (custompanoramaproperties != null)
        {
            l = custompanoramaproperties.getOverlay1Top();
            i1 = custompanoramaproperties.getOverlay1Bottom();
            j1 = custompanoramaproperties.getOverlay2Top();
            k1 = custompanoramaproperties.getOverlay2Bottom();
        }

        if (l != 0 || i1 != 0)
        {
            this.drawGradientRect(0, 0, this.width, this.height, l, i1);
        }

        if (j1 != 0 || k1 != 0)
        {
            this.drawGradientRect(0, 0, this.width, this.height, j1, k1);
        }
        */
		//
        this.mc.getTextureManager().bindTexture(MINECRAFT_TITLE_TEXTURES);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        /* MINECRAFT title
        if ((double)this.updateCounter < 1.0E-4D)
        {
            this.drawTexturedModalRect(j + 0, 30, 0, 0, 99, 44);
            this.drawTexturedModalRect(j + 99, 30, 129, 0, 27, 44);
            this.drawTexturedModalRect(j + 99 + 26, 30, 126, 0, 3, 44);
            this.drawTexturedModalRect(j + 99 + 26 + 3, 30, 99, 0, 26, 44);
            this.drawTexturedModalRect(j + 155, 30, 0, 45, 155, 44);
        }
        else
        {
            this.drawTexturedModalRect(j + 0, 30, 0, 0, 155, 44);
            this.drawTexturedModalRect(j + 155, 30, 0, 45, 155, 44);
        }
        */

        if (Reflector.ForgeHooksClient_renderMainMenu.exists())
        {
            this.splashText = Reflector.callString(Reflector.ForgeHooksClient_renderMainMenu, new Object[] {this, this.fontRendererObj, Integer.valueOf(this.width), Integer.valueOf(this.height), this.splashText});
        }

        GlStateManager.pushMatrix();
        //GlStateManager.translate((float)(this.width / 2 + 90), 70.0F, 0.0F);
        //GlStateManager.rotate(0.0F, 0.0F, 0.0F, 1.0F);
        float f = 1.8F - MathHelper.abs(MathHelper.sin((float)(Minecraft.getSystemTime() % 1000L) / 1000.0F * ((float)Math.PI * 2F)) * 0.1F);
        f = f * 100.0F / (float)(this.fontRendererObj.getStringWidth(this.splashText) + 32);
        GlStateManager.scale(1F, 1F, f);
        this.drawCenteredString(this.fontRendererObj, this.splashText, width/2, this.height / 4 + 32, -256);
        GlStateManager.popMatrix();
        String s = "Minecraft 1.11";

        if (this.mc.isDemo())
        {
            s = s + " Demo";
        }
        else
        {
            s = s + ("release".equalsIgnoreCase(this.mc.getVersionType()) ? "" : "/" + this.mc.getVersionType());
        }
        
        //TODO: Client MainMessage
        s = Shape.parseVersion(s);

        if (Reflector.FMLCommonHandler_getBrandings.exists())
        {
            Object object = Reflector.call(Reflector.FMLCommonHandler_instance, new Object[0]);
            List<String> list = Lists.<String>reverse((List)Reflector.call(object, Reflector.FMLCommonHandler_getBrandings, new Object[] {Boolean.valueOf(true)}));

            for (int l1 = 0; l1 < list.size(); ++l1)
            {
                String s1 = (String)list.get(l1);

                if (!Strings.isNullOrEmpty(s1))
                {
                    this.drawString(this.fontRendererObj, s1, 2, this.height - (10 + l1 * (this.fontRendererObj.FONT_HEIGHT + 1)), 16777215);
                }
            }
        }
        else
        {
            this.drawString(this.fontRendererObj, s, 2, this.height - 10, -1);
        }

        String s2 = "Copyright Mojang AB. Do not distribute!";
        s2 = Shape.parseCopy(s2);
        this.drawString(this.fontRendererObj, s2, this.width - this.fontRendererObj.getStringWidth(s2) - 2, this.height - 10, -1);

        //TODO: CLIENT INPUT INVOKE
        this.drawString(this.fontRendererObj, this.shapeinput, this.width/2 - this.fontRendererObj.getStringWidth(this.shapeinput)/2, this.height - 10, -1);
        
        if (this.openGLWarning1 != null && !this.openGLWarning1.isEmpty())
        {
            drawRect(this.openGLWarningX1 - 2, this.openGLWarningY1 - 2, this.openGLWarningX2 + 2, this.openGLWarningY2 - 1, 1428160512);
            this.drawString(this.fontRendererObj, this.openGLWarning1, this.openGLWarningX1, this.openGLWarningY1, -1);
            this.drawString(this.fontRendererObj, this.openGLWarning2, (this.width - this.openGLWarning2Width) / 2, ((GuiButton)this.buttonList.get(0)).yPosition - 12, -1);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (this.areRealmsNotificationsEnabled())
        {
            this.realmsNotification.drawScreen(mouseX, mouseY, partialTicks);
        }

        if (this.modUpdateNotification != null)
        {
            this.modUpdateNotification.drawScreen(mouseX, mouseY, partialTicks);
        }
        
        if (Shape.INSTANCE.NEtb != null && Shape.INSTANCE.themeManager.getTheme().windows98Taskbar)
        {
			Minecraft.getMinecraft().getTextureManager()
			.bindTexture(Shape.INSTANCE.NEtb);
        	glEnable(GL_BLEND);
        	glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        	
        	Integer h = 14;
        	
        	drawScaledCustomSizeModalRect(0, height-h, 0, 805, 208, 28, 208/2, 14, 1433, 805+28);
        	drawScaledCustomSizeModalRect(width-211/2, height-h, 1222, 805, 211, 28, 211/2, 14, 1433, 805+28);
        	
        	Integer ik = 104;
        	while (ik < width-211/2)
        	{
        		drawScaledCustomSizeModalRect(ik, height-h, 805, 805, 1, 28, 1, 14, 1433, 805+28);
        		ik += 1;
        	}
        	
        	//System.out.println("HEIGHT: " + height);
        	
        	glDisable(GL_BLEND);
        }
        
        if (Shape.INSTANCE.XPtb != null && Shape.INSTANCE.themeManager.getTheme().windowsXPTaskbar)
        {
			Minecraft.getMinecraft().getTextureManager()
			.bindTexture(Shape.INSTANCE.XPtb);
        	glEnable(GL_BLEND);
        	glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        	
        	Integer h = 14;
        	
        	drawScaledCustomSizeModalRect(0, height-h, 0, 805, 208, 28, 208/2, 14, 1433, 805+28);
        	drawScaledCustomSizeModalRect(width-211/2, height-h, 1222, 805, 211, 28, 211/2, 14, 1433, 805+28);
        	
        	Integer ik = 104;
        	while (ik < width-211/2)
        	{
        		drawScaledCustomSizeModalRect(ik, height-h, 805, 805, 1, 28, 1, 14, 1433, 805+28);
        		ik += 1;
        	}
        	
        	//System.out.println("HEIGHT: " + height);
        	
        	glDisable(GL_BLEND);
        }
    }

    /**
     * Called when the mouse is clicked. Args : mouseX, mouseY, clickedButton
     */
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        synchronized (this.threadLock)
        {
            if (!this.openGLWarning1.isEmpty() && !StringUtils.isNullOrEmpty(this.openGLWarningLink) && mouseX >= this.openGLWarningX1 && mouseX <= this.openGLWarningX2 && mouseY >= this.openGLWarningY1 && mouseY <= this.openGLWarningY2)
            {
                GuiConfirmOpenLink guiconfirmopenlink = new GuiConfirmOpenLink(this, this.openGLWarningLink, 13, true);
                guiconfirmopenlink.disableSecurityWarning();
                this.mc.displayGuiScreen(guiconfirmopenlink);
            }
        }

        if (this.areRealmsNotificationsEnabled())
        {
            this.realmsNotification.mouseClicked(mouseX, mouseY, mouseButton);
        }

        if (Reflector.ForgeHooksClient_mainMenuMouseClick.exists())
        {
            Reflector.call(Reflector.ForgeHooksClient_mainMenuMouseClick, new Object[] {Integer.valueOf(mouseX), Integer.valueOf(mouseY), Integer.valueOf(mouseButton), this.fontRendererObj, Integer.valueOf(this.width)});
        }
    }

    /**
     * Called when the screen is unloaded. Used to disable keyboard repeat events
     */
    public void onGuiClosed()
    {
        if (this.realmsNotification != null)
        {
            this.realmsNotification.onGuiClosed();
        }
    }
}
