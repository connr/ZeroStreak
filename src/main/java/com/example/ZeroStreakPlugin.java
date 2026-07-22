package com.example;

import com.google.inject.Provides;
import javax.inject.Inject;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.api.Hitsplat;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
		name = "Zero Streak",
		description = "Plays a sound when you hit too many zeros in a row",
		tags = {"combat", "sound", "zero", "splash", "hitsplat"}
)
public class ZeroStreakPlugin extends Plugin
{
	private static final File RUNELITE_DIR = new File(System.getProperty("user.home"), ".runelite");

	@Inject
	private ZeroStreakConfig config;
	private int consecutiveZeros = 0;


	@Provides
	ZeroStreakConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ZeroStreakConfig.class);
	}

	@Override
	protected void startUp()
	{
		consecutiveZeros = 0;
		createCustomSoundFolder();
	}

	private void createCustomSoundFolder()
	{
		File soundFolder = new File(RUNELITE_DIR, "zerostreak");
		if (!soundFolder.exists())
		{
			soundFolder.mkdirs();
		}

		File customSound = new File(soundFolder, "zero_streak.wav");
		if (!customSound.exists())
		{
			try (InputStream in = ZeroStreakPlugin.class.getResourceAsStream("zero_streak.wav");
				 FileOutputStream out = new FileOutputStream(customSound))
			{
				if (in == null)
				{
					log.warn("Could not find bundled zero_streak.wav to copy");
					return;
				}
				byte[] buffer = new byte[1024];
				int bytesRead;
				while ((bytesRead = in.read(buffer)) != -1)
				{
					out.write(buffer, 0, bytesRead);
				}
			}
			catch (IOException e)
			{
				log.warn("Failed to copy bundled sound to .runelite/zerostreak/", e);
			}
		}
	}

	@Override
	protected void shutDown()
	{
		consecutiveZeros = 0;
		log.info("Zero Streak plugin stopped");
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		// Only care about hitsplats landing on NPCs, not on the player
		if (!(event.getActor() instanceof NPC))
		{
			return;
		}

		Hitsplat hitsplat = event.getHitsplat();

		if (!hitsplat.isMine())
		{
			return;
		}

		int amount = hitsplat.getAmount();

		if (amount == 0)
		{
			consecutiveZeros++;
			log.debug("Consecutive zeros: {}", consecutiveZeros);

			if (consecutiveZeros >= config.consecutiveZerosRequired())
			{
				consecutiveZeros = 0;
				playSound();
			}
		}
		else
		{
			consecutiveZeros = 0;
		}
	}

	private void playSound()
	{
		log.warn("playSound called, useCustomSound={}", config.useCustomSound());
		File customSound = new File(RUNELITE_DIR, "zerostreak/zero_streak.wav");

		try
		{
			AudioInputStream audioStream;

			if (config.useCustomSound() && customSound.exists())
			{
				audioStream = AudioSystem.getAudioInputStream(customSound);
			}
			else
			{
				InputStream resourceStream = ZeroStreakPlugin.class.getResourceAsStream("/com/beard/zero_streak.wav");
				if (resourceStream == null)
				{
					log.warn("Could not find bundled zero_streak.wav");
					return;
				}
				audioStream = AudioSystem.getAudioInputStream(new BufferedInputStream(resourceStream));
			}

			Clip clip = AudioSystem.getClip();
			clip.open(audioStream);

			if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN))
			{
				FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
				float volume = config.volume() / 100f;
				float dB = (float) (Math.log(Math.max(volume, 0.0001)) / Math.log(10.0) * 20.0);
				dB = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB));
				gainControl.setValue(dB);
			}

			clip.start();

			clip.addLineListener(e ->
			{
				if (e.getType() == LineEvent.Type.STOP)
				{
					clip.close();
					try
					{
						audioStream.close();
					}
					catch (IOException ex)
					{
						log.warn("Error closing audio stream", ex);
					}
				}
			});
		}
		catch (UnsupportedAudioFileException | LineUnavailableException | IOException e)
		{
			log.warn("Failed to play zero streak sound", e);
		}
	}
}