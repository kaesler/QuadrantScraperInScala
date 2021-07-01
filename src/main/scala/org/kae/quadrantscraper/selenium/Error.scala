package org.kae.quadrantscraper.selenium

sealed trait Error         extends Throwable
case object DownloadFailed extends Error
