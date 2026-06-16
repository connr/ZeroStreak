package com.beard;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ZeroStreakTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(com.example.ZeroStreakPlugin.class);
		RuneLite.main(args);
	}
}