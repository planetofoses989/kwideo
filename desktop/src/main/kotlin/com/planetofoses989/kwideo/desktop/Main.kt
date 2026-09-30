package com.planetofoses989.kwideo.desktop

import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

/**
 * Kwideo for Linux — early desktop scaffold (Phase 1 of the Linux port).
 *
 * This is a placeholder window only. Video playback, library browsing and
 * settings are not yet ported from the Android app; see docs/linux-port.md
 * for the phase plan. Uses only JDK Swing so Phase 1 needs no dependencies
 * beyond the Kotlin JVM plugin.
 */
fun main() {
  SwingUtilities.invokeLater {
    val frame = JFrame("Kwideo — Linux (early scaffold)")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.minimumSize = Dimension(640, 400)
    val label =
      JLabel(
        "<html><center><h1>Kwideo for Linux</h1>" +
          "<p>Early desktop scaffold — playback not yet ported.</p>" +
          "<p>See docs/linux-port.md for the port plan.</p></center></html>",
        SwingConstants.CENTER,
      )
    frame.contentPane.add(label, BorderLayout.CENTER)
    frame.pack()
    frame.setLocationRelativeTo(null)
    frame.isVisible = true
  }
}
