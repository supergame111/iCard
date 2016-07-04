/* NFCard is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

NFCard is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Wget.  If not, see <http://www.gnu.org/licenses/>.

Additional permission under GNU GPL version 3 section 7 */

package com.ftsafe.iccd.ecard.ui.pages;

import android.app.Activity;
import android.content.Intent;

import com.ftsafe.iccd.ecard.App;
import com.ftsafe.iccd.ecard.R;

public final class AboutPage {
	private static final String TAG = "ABOUTPAGE_ACTION";

	public static CharSequence getContent(Activity activity) {

		String tip = App
				.getStringResource(R.string.info_main_about);
		tip = tip.replace("<app />", App.name());
		tip = tip.replace("<version />", App.version());

		return new SpanFormatter(null).toSpanned(tip);
	}

	public static boolean isSendByMe(Intent intent) {
		return intent != null && TAG.equals(intent.getAction());
	}

	static SpanFormatter.ActionHandler getActionHandler(Activity activity) {
		return new Handler(activity);
	}

	private static final class Handler implements SpanFormatter.ActionHandler {
		private final Activity activity;

		Handler(Activity activity) {
			this.activity = activity;
		}

		@Override
		public void handleAction(CharSequence name) {
			activity.setIntent(new Intent(TAG));
		}
	}

	private AboutPage() {
	}
}
