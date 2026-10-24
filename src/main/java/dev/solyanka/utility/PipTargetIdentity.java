/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility;
/** Window ownership identity; titles deliberately excluded. No executable-name retargeting. */
public record PipTargetIdentity(String window, int pid, long processStart) {
 public boolean matches(PipTargetIdentity current) {
  return current != null && window != null && !window.isBlank() && pid > 0
    && window.equals(current.window) && pid == current.pid
    && (processStart < 0 || processStart == current.processStart);
 }
}
