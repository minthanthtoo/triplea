package games.strategy.engine.framework.map.download;

import java.io.File;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.settings.SystemPreferenceKey;
import games.strategy.triplea.settings.SystemPreferences;
import games.strategy.ui.SwingComponents;
import games.strategy.util.Version;

/** Controller for in-game map download actions. */
public class MapDownloadController {

  private final MapListingSource mapDownloadProperties;

  public MapDownloadController(final MapListingSource mapSource) {
    mapDownloadProperties = mapSource;
  }

  /**
   * Return true if all locally downloaded maps are latest versions, false if any can are out of date or their version
   * not recognized.
   */
  public boolean checkDownloadedMapsAreLatest() {
    try {
      // check at most once per month
      final Calendar calendar = Calendar.getInstance();
      final int year = calendar.get(Calendar.YEAR);
      final int month = calendar.get(Calendar.MONTH);
      // format year:month
      final String lastCheckTime = SystemPreferences.get(SystemPreferenceKey.TRIPLEA_LAST_CHECK_FOR_MAP_UPDATES, "");
      if (lastCheckTime != null && lastCheckTime.trim().length() > 0) {
        final String[] yearMonth = lastCheckTime.split(":");
        if (Integer.parseInt(yearMonth[0]) >= year && Integer.parseInt(yearMonth[1]) >= month) {
          return false;
        }
      }

      SystemPreferences.put(SystemPreferenceKey.TRIPLEA_LAST_CHECK_FOR_MAP_UPDATES, year + ":" + month);

      final Collection<DownloadFileDescription> allDownloads =
          new DownloadRunnable(mapDownloadProperties.getMapListDownloadSite()).getDownloads();
      final Collection<String> outOfDateMapNames = getOutOfDateMapNames(allDownloads);
      if (!outOfDateMapNames.isEmpty()) {
        final StringBuilder text = new StringBuilder();
        text.append("<html>Some of the maps you have are out of date, and newer versions of those maps exist.<br><br>");
        text.append("Would you like to update (re-download) the following maps now?<br><ul>");
        for (final String mapName : outOfDateMapNames) {
          text.append("<li> ").append(mapName).append("</li>");
        }
        text.append("</ul></html>");
        SwingComponents.promptUser("Update Your Maps?", text.toString(),
            () -> DownloadMapsWindow.showDownloadMapsWindowAndDownload(outOfDateMapNames));
        return true;
      }
    } catch (final Exception e) {
      ClientLogger.logError("Error while checking for map updates", e);
    }
    return false;
  }

  private static Collection<String> getOutOfDateMapNames(final Collection<DownloadFileDescription> downloads) {
    return getOutOfDateMapNames(downloads, getDownloadedMaps());
  }

  @VisibleForTesting
  static Collection<String> getOutOfDateMapNames(
      final Collection<DownloadFileDescription> downloads,
      final DownloadedMaps downloadedMaps) {
    return downloads.stream()
        .filter(Objects::nonNull)
        .filter(it -> isMapOutOfDate(it, downloadedMaps))
        .map(DownloadFileDescription::getMapName)
        .collect(Collectors.toList());
  }

  private static boolean isMapOutOfDate(final DownloadFileDescription download, final DownloadedMaps downloadedMaps) {
    final Optional<Version> latestVersion = Optional.ofNullable(download.getVersion());
    final Optional<Version> downloadedVersion = getDownloadedVersion(download.getMapName(), downloadedMaps);

    final AtomicBoolean mapOutOfDate = new AtomicBoolean(false);
    latestVersion.ifPresent(latest -> {
      downloadedVersion.ifPresent(downloaded -> {
        mapOutOfDate.set(latest.isGreaterThan(downloaded));
      });
    });
    return mapOutOfDate.get();
  }

  private static Optional<Version> getDownloadedVersion(final String mapName, final DownloadedMaps downloadedMaps) {
    return downloadedMaps.getZipFileCandidates(mapName).stream()
        .map(downloadedMaps::getVersionForZipFile)
        .filter(Optional::isPresent)
        .findFirst()
        .orElseGet(Optional::empty);
  }

  @VisibleForTesting
  interface DownloadedMaps {
    Optional<Version> getVersionForZipFile(File mapZipFile);

    List<File> getZipFileCandidates(String mapName);
  }

  private static DownloadedMaps getDownloadedMaps() {
    return new DownloadedMaps() {
      @Override
      public Optional<Version> getVersionForZipFile(final File mapZipFile) {
        return Optional.ofNullable(DownloadFileProperties.loadForZip(mapZipFile).getVersion());
      }

      @Override
      public List<File> getZipFileCandidates(final String mapName) {
        return ResourceLoader.getMapZipFileCandidates(mapName);
      }
    };
  }

  /**
   * Indicates the user should be prompted to download the tutorial map.
   *
   * @return {@code true} if the user should be prompted to download the tutorial map; otherwise {@code false}.
   */
  public boolean shouldPromptToDownloadTutorialMap() {
    return shouldPromptToDownloadTutorialMap(getTutorialMapPreferences(), getUserMaps());
  }

  @VisibleForTesting
  static boolean shouldPromptToDownloadTutorialMap(
      final TutorialMapPreferences tutorialMapPreferences,
      final UserMaps userMaps) {
    return tutorialMapPreferences.canPromptToDownload() && userMaps.isEmpty();
  }

  @VisibleForTesting
  interface TutorialMapPreferences {
    boolean canPromptToDownload();

    void preventPromptToDownload();
  }

  private static TutorialMapPreferences getTutorialMapPreferences() {
    return new TutorialMapPreferences() {
      @Override
      public void preventPromptToDownload() {
        SystemPreferences.put(SystemPreferenceKey.TRIPLEA_PROMPT_TO_DOWNLOAD_TUTORIAL_MAP, false);
      }

      @Override
      public boolean canPromptToDownload() {
        return SystemPreferences.get(SystemPreferenceKey.TRIPLEA_PROMPT_TO_DOWNLOAD_TUTORIAL_MAP, true);
      }
    };
  }

  @VisibleForTesting
  interface UserMaps {
    boolean isEmpty();
  }

  private static UserMaps getUserMaps() {
    return new UserMaps() {
      @Override
      public boolean isEmpty() {
        final String[] entries = ClientFileSystemHelper.getUserMapsFolder().list();
        final int entryCount = Optional.ofNullable(entries).map(it -> it.length).orElse(0);
        return entryCount == 0;
      }
    };
  }

  /**
   * Prevents the user from being prompted to download the tutorial map.
   */
  public void preventPromptToDownloadTutorialMap() {
    preventPromptToDownloadTutorialMap(getTutorialMapPreferences());
  }

  @VisibleForTesting
  static void preventPromptToDownloadTutorialMap(final TutorialMapPreferences tutorialMapPreferences) {
    tutorialMapPreferences.preventPromptToDownload();
  }
}
