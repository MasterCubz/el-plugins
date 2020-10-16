/*
 * Copyright (c) 2018, Andrew EP | ElPinche256 <https://github.com/ElPinche256>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.ElCosmics;

import net.runelite.client.config.*;

@ConfigGroup("ElCosmics")

public interface ElCosmicsConfig extends Config
{

	@ConfigTitleSection(
			keyName = "instructionsTitle",
			name = "Instructions",
			description = "",
			position = 0
	)
	default Title instructionsTitle()
	{
		return new Title();
	}

	@ConfigItem(
			keyName = "instructions",
			name = "",
			description = "Instructions. Don't enter anything into this field",
			position = 0,
			titleSection = "instructionsTitle"
	)
	default String instructions()
	{
		return "Not sure yet";
	}

	@ConfigTitleSection(
			keyName = "generalTitle",
			name = "General Config",
			description = "",
			position = 1
	)
	default Title generalTitle()
	{
		return new Title();
	}

	@ConfigItem(
			keyName = "giantPouch",
			name = "Use Giant Pouch",
			description = "Use giant pouch",
			position = 1,
			titleSection = "generalTitle"
	)
	default boolean giantPouch() { return false; }

	@ConfigItem(
			keyName = "daeyalt",
			name = "Use Daeyalt Essence",
			description = "Use daeyalt essence",
			position = 2,
			titleSection = "generalTitle"
	)
	default boolean daeyalt() { return false; }

	@ConfigItem(
			keyName = "dreamMentor",
			name = "Dream Mentor Completed",
			description = "Tick this if you have Dream Mentor Completed.",
			position = 3,
			titleSection = "generalTitle"
	)
	default boolean dreamMentor() { return false; }

	@ConfigItem(
			keyName = "noStams",
			name = "No Staminas",
			description = "Tick this if you don't have any stamina potions.",
			position = 5,
			titleSection = "generalTitle"
	)
	default boolean noStams() { return false; }

	@ConfigItem(
			keyName = "minEnergy",
			name = "Minimum Energy",
			description = "Minimum energy before stam pot drank",
			position = 13,
			hidden = false,
			hide = "noStams",
			titleSection = "generalTitle"
	)
	default int minEnergy() { return 50; }

	@ConfigItem(
			keyName = "minHealth",
			name = "Minimum Health",
			description = "Minimum health before food eaten",
			position = 16,
			titleSection = "generalTitle"
	)
	default int minHealth() { return 65; }

	@ConfigItem(
			keyName = "pauseHealth",
			name = "Pause Health",
			description = "Health below which the plugin will pause.",
			position = 17
	)
	default int pauseHealth() { return 40; }

	@ConfigItem(
			keyName = "foodType",
			name = "Food ID",
			description = "ID of food to eat",
			position = 15,
			titleSection = "generalTitle"
	)
	default int foodId() { return 7946; }

	@ConfigTitleSection(
			keyName = "delayTitle",
			name = "Delay Config",
			description = "",
			position = 40
	)
	default Title delayTitle()
	{
		return new Title();
	}

	@ConfigItem(
			keyName = "sleepMin",
			name = "Sleep Min",
			description = "Enter minimum sleep delay here.",
			position = 41,
			titleSection = "delayTitle"
	)
	default int sleepMin()
	{
		return 60;
	}

	@ConfigItem(
			keyName = "sleepMax",
			name = "Sleep Max",
			description = "Enter maximum sleep delay here.",
			position = 42,
			titleSection = "delayTitle"
	)
	default int sleepMax()
	{
		return 350;
	}

	@ConfigItem(
			keyName = "sleepDeviation",
			name = "Sleep Deviation",
			description = "Enter sleep delay deviation here.",
			position = 43,
			titleSection = "delayTitle"
	)
	default int sleepDeviation()
	{
		return 100;
	}

	@ConfigItem(
			keyName = "sleepTarget",
			name = "Sleep Target",
			description = "Enter target sleep delay here.",
			position = 44,
			titleSection = "delayTitle"
	)
	default int sleepTarget()
	{
		return 100;
	}

	@ConfigItem(
			keyName = "tickMin",
			name = "Tick Min",
			description = "Enter minimum tick delay here.",
			position = 45,
			titleSection = "delayTitle"
	)
	default int tickMin()
	{
		return 1;
	}

	@ConfigItem(
			keyName = "tickMax",
			name = "Tick Max",
			description = "Enter maximum tick delay here.",
			position = 46,
			titleSection = "delayTitle"
	)
	default int tickMax()
	{
		return 3;
	}

	@ConfigItem(
			keyName = "tickDeviation",
			name = "Tick Deviation",
			description = "Enter tick delay deviation here.",
			position = 47,
			titleSection = "delayTitle"
	)
	default int tickDeviation()
	{
		return 2;
	}

	@ConfigItem(
			keyName = "tickTarget",
			name = "Tick Target",
			description = "Enter target tick delay here.",
			position = 48,
			titleSection = "delayTitle"
	)
	default int tickTarget()
	{
		return 2;
	}

	@ConfigTitleSection(
			keyName = "uiTitle",
			name = "UI Config",
			description = "",
			position = 140
	)
	default Title uiTitle()
	{
		return new Title();
	}

	@ConfigItem(
			keyName = "enableUI",
			name = "Enable UI",
			description = "Enable to turn on in game UI",
			position = 140,
			titleSection = "uiTitle"
	)
	default boolean enableUI()
	{
		return true;
	}

	@ConfigItem(
			keyName = "startButton",
			name = "Start/Stop",
			description = "Test button that changes variable value",
			position = 150
	)
	default Button startButton()
	{
		return new Button();
	}
}