/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *   Copyright (c) 2013-2015 Anatolij Zelenin, Georg Semmler.
 *
 *       This program is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       any later version.
 *
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU General Public License for more details.
 *
 *       You should have received a copy of the GNU General Public License
 *       along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package de.azapps.mirakel.settings.model_settings.special_list.helper;

import android.content.Context;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.base.Optional;

import de.azapps.mirakel.model.list.meta.SpecialListsBaseProperty;
import de.azapps.mirakel.settings.R;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

public class SpecialListsViewHelper {

    @NonNull
    private Type type;
    @NonNull
    private Optional<SpecialListsBaseProperty> condition = absent();
    @NonNull
    private Optional<Preference> preference = absent();
    @NonNull
    private final Context context;

    public SpecialListsViewHelper(final @NonNull SpecialListsBaseProperty condition,
                                  final @NonNull Context ctx) {
        this.condition = of(condition);
        this.type = Type.CONDITION;
        this.context = ctx;
    }

    public SpecialListsViewHelper(final @NonNull Preference preference, final @NonNull Context ctx) {
        this.preference = of(preference);
        this.type = Type.PREFERENCE;
        this.context = ctx;
    }

    @NonNull
    public Type getType() {
        return type;
    }

    @NonNull
    public Optional<SpecialListsBaseProperty> getCondition() {
        return condition;
    }

    @NonNull
    public Optional<Preference> getPreference() {
        return preference;
    }

    public View getView(View convertView, final @NonNull LayoutInflater inflater,
                        final @NonNull ViewGroup parent) {
        if (type == Type.PREFERENCE) {
            return preference.get().getView(null,
                                            parent); //need to be null else the categories tries to reuse the condition view
        } else {
            ConditionViewHolder holder;
            if (convertView == null || !(convertView.getTag() instanceof ConditionViewHolder)) {
                convertView = inflater.inflate(android.R.layout.simple_list_item_2, null);
                holder = new ConditionViewHolder(convertView);
                convertView.setTag(holder);
                convertView.setId(R.id.remove_handle);
            } else {
                holder = (ConditionViewHolder) convertView.getTag();
            }
            holder.title.setText(condition.get().getTitle(context));
            holder.summary.setText(condition.get().getSummary(context));
            return convertView;
        }
    }

    protected enum Type {
        PREFERENCE, CONDITION
    }


    private static class ConditionViewHolder {
        TextView title;
        TextView summary;

        public ConditionViewHolder(final @NonNull View v) {
            title = (TextView)v.findViewById(android.R.id.text1);
            summary = (TextView)v.findViewById(android.R.id.text2);
        }
    }
}
