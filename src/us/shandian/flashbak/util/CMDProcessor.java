/*
 * Copyright (C) 2011 SlimRoms Project
 * Copyright (C) 2013 FlashBak
 * Not a contribution
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package us.shandian.flashbak.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

import android.util.Log;
import android.content.Context;

import us.shandian.flashbak.R;

public class CMDProcessor {

	private static final String TAG = "CMD Processor";
	private Boolean can_su;
	public SH sh;
	public SH su;
	public static String BUSYBOX = "busybox";

	public CMDProcessor() {
		sh = new SH("sh");
		su = new SH("su");
	}

	public SH suOrSH() {
		return canSU() ? su : sh;
	}

	public boolean canSU() {
		return canSU(false);
	}

	public class CommandResult {
		public final String stdout;
		public final String stderr;
		public final Integer exit_value;

		CommandResult(final Integer exit_value_in) {
			this(exit_value_in, null, null);
		}

		CommandResult(final Integer exit_value_in, final String stdout_in,
					  final String stderr_in) {
			exit_value = exit_value_in;
			stdout = stdout_in;
			stderr = stderr_in;
		}

		public boolean success() {
			return exit_value != null && exit_value == 0;
		}
	}

	public class SH {
		private String SHELL = "sh";

		public SH(final String SHELL_in) {
			SHELL = SHELL_in;
		}

		private String getStreamLines(final InputStream is) {
			String out = null;
			StringBuffer buffer = null;
			final DataInputStream dis = new DataInputStream(is);

			try {
				if (dis.available() > 0) {
					buffer = new StringBuffer(dis.readLine());
					while (dis.available() > 0) {
						buffer.append("\n").append(dis.readLine());
					}
				}
				dis.close();
			} catch (final Exception ex) {
				Log.e(TAG, ex.getMessage());
			}
			if (buffer != null) {
				out = buffer.toString();
			}
			return out;
		}

		public Process run(final String s) {
			Process process = null;
			try {
				process = Runtime.getRuntime().exec(SHELL);
				final DataOutputStream toProcess = new DataOutputStream(
					process.getOutputStream());
				toProcess.writeBytes("exec " + s.replaceFirst("busybox", BUSYBOX) + "\n");
				toProcess.flush();
			} catch (final Exception e) {
				Log.e(TAG, "Exception while trying to run: '" + s + "' "
					  + e.getMessage());
				process = null;
			}
			return process;
		}

		public CommandResult runWaitFor(final String s) {
			final Process process = run(s);
			Integer exit_value = null;
			String stdout = null;
			String stderr = null;
			if (process != null) {
				try {
					exit_value = process.waitFor();

					stdout = getStreamLines(process.getInputStream());
					stderr = getStreamLines(process.getErrorStream());

				} catch (final InterruptedException e) {
					Log.e(TAG, "runWaitFor " + e.toString());
				} catch (final NullPointerException e) {
					Log.e(TAG, "runWaitFor " + e.toString());
				}
			}
			return new CommandResult(exit_value, stdout, stderr);
		}
	}

	public boolean canSU(final boolean force_check) {
		if (can_su == null || force_check) {
			final CommandResult r = su.runWaitFor("id");
			final StringBuilder out = new StringBuilder();

			if (r.stdout != null) {
				out.append(r.stdout).append(" ; ");
			}
			if (r.stderr != null) {
				out.append(r.stderr);
			}

			Log.d(TAG, "canSU() su[" + r.exit_value + "]: " + out);
			can_su = r.success();
		}
		return can_su;
	}

	public static boolean getMount(final String mount) {
		final CMDProcessor cmd = new CMDProcessor();
		final String mounts[] = getMounts("/system");
		if (mounts != null
			&& mounts.length >= 3) {
			final String device = mounts[0];
			final String path = mounts[1];
			final String point = mounts[2];
			if (cmd.su.runWaitFor("mount -o " + mount + ",remount -t "
								  + point + " " + device + " " + path).success()) {
				return true;
			}
		}
		return ( cmd.su.runWaitFor("busybox mount -o remount," + mount + " /system").success() );
	}


	public static String[] getMounts(final String path) {
		try {
			BufferedReader br = new BufferedReader(new FileReader("/proc/mounts"), 256);
			String line = null;
			while ((line = br.readLine()) != null) {
				if (line.contains(path)) {
					return line.split(" ");
				}
			}
			br.close();
		}
		catch (FileNotFoundException e) {
			Log.d(TAG, "/proc/mounts does not exist");
		}
		catch (IOException e) {
			Log.d(TAG, "Error reading /proc/mounts");
		}
		return null;
	}
	
	public static void restartSystemUI() {
		new CMDProcessor().su.run("pkill -TERM -f com.android.systemui");
	}
	
	public static void exportBusybox(Context context) {
		File busybox = new File(context.getFilesDir().getPath() + "/busybox");
		if (!busybox.exists()) {
			FileOutputStream opt;
			DataInputStream ipt = new DataInputStream(context.getResources().openRawResource(R.raw.busybox));
			byte[] bytes;
			try {
				busybox.createNewFile();
				opt = new FileOutputStream(busybox);
				bytes = new byte[ipt.available()];
				ipt.readFully(bytes);
				opt.write(bytes);
			} catch (IOException e) {
				busybox.delete();
				e.printStackTrace();
				return;
			}
			
			if(!new CMDProcessor().sh.runWaitFor("chmod 0777 " + busybox.getPath()).success()) {
				busybox.delete();
			} else {
				BUSYBOX = busybox.getPath();
			}
			
			try {
				opt.close();
				ipt.close();
			} catch (IOException e) {
				// So?
			}
		}
	}
}

