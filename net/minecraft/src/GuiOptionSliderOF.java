package net.minecraft.src;

import net.minecraft.client.gui.GuiOptionSlider;
import net.minecraft.client.settings.GameSettings;

public class GuiOptionSliderOF extends GuiOptionSlider implements IOptionControl
{
    private GameSettings.Options option = null;

    public GuiOptionSliderOF(int p_i40_1_, int p_i40_2_, int p_i40_3_, GameSettings.Options p_i40_4_)
    {
        super(p_i40_1_, p_i40_2_, p_i40_3_, p_i40_4_);
        this.option = p_i40_4_;
    }

    public GameSettings.Options getOption()
    {
        return this.option;
    }
}
