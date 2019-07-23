/*
 * Copyright (C) 2019 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.genonbeta.TrebleShot.object;

import android.content.ContentValues;

import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.DatabaseObject;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;

import java.util.List;

public class NetworkDevice implements DatabaseObject<Object>
{
    public String brand;
    public String model;
    public String nickname;
    public String id;
    public String versionName;
    public int versionNumber;
    public int tmpSecureKey;
    public long lastUsageTime;
    public boolean isTrusted = false;
    public boolean isRestricted = false;
    public boolean isLocalAddress = false;
    public Type type = Type.NORMAL;

    public NetworkDevice()
    {
    }

    public NetworkDevice(String id)
    {
        this.id = id;
    }

    public NetworkDevice(CursorItem item)
    {
        reconstruct(item);
    }

    public String generatePictureId()
    {
        return String.format("picture_%s", id);
    }

    @Override
    public SQLQuery.Select getWhere()
    {
        return new SQLQuery.Select(AccessDatabase.TABLE_DEVICES)
                .setWhere(AccessDatabase.FIELD_DEVICES_ID + "=?", id);
    }

    public ContentValues getValues()
    {
        ContentValues values = new ContentValues();

        values.put(AccessDatabase.FIELD_DEVICES_ID, id);
        values.put(AccessDatabase.FIELD_DEVICES_USER, nickname);
        values.put(AccessDatabase.FIELD_DEVICES_BRAND, brand);
        values.put(AccessDatabase.FIELD_DEVICES_MODEL, model);
        values.put(AccessDatabase.FIELD_DEVICES_BUILDNAME, versionName);
        values.put(AccessDatabase.FIELD_DEVICES_BUILDNUMBER, versionNumber);
        values.put(AccessDatabase.FIELD_DEVICES_LASTUSAGETIME, lastUsageTime);
        values.put(AccessDatabase.FIELD_DEVICES_ISRESTRICTED, isRestricted ? 1 : 0);
        values.put(AccessDatabase.FIELD_DEVICES_ISTRUSTED, isTrusted ? 1 : 0);
        values.put(AccessDatabase.FIELD_DEVICES_ISLOCALADDRESS, isLocalAddress ? 1 : 0);
        values.put(AccessDatabase.FIELD_DEVICES_TMPSECUREKEY, tmpSecureKey);
        values.put(AccessDatabase.FIELD_DEVICES_EXTRA_TYPE, type.toString());

        return values;
    }

    @Override
    public void reconstruct(CursorItem item)
    {
        this.id = item.getString(AccessDatabase.FIELD_DEVICES_ID);
        this.nickname = item.getString(AccessDatabase.FIELD_DEVICES_USER);
        this.brand = item.getString(AccessDatabase.FIELD_DEVICES_BRAND);
        this.model = item.getString(AccessDatabase.FIELD_DEVICES_MODEL);
        this.versionName = item.getString(AccessDatabase.FIELD_DEVICES_BUILDNAME);
        this.versionNumber = item.getInt(AccessDatabase.FIELD_DEVICES_BUILDNUMBER);
        this.lastUsageTime = item.getLong(AccessDatabase.FIELD_DEVICES_LASTUSAGETIME);
        this.isTrusted = item.getInt(AccessDatabase.FIELD_DEVICES_ISTRUSTED) == 1;
        this.isRestricted = item.getInt(AccessDatabase.FIELD_DEVICES_ISRESTRICTED) == 1;
        this.isLocalAddress = item.getInt(AccessDatabase.FIELD_DEVICES_ISLOCALADDRESS) == 1;
        this.tmpSecureKey = item.getInt(AccessDatabase.FIELD_DEVICES_TMPSECUREKEY);

        try {
            this.type = Type.valueOf(item.getString(AccessDatabase.FIELD_DEVICES_EXTRA_TYPE));
        } catch (Exception e) {
            this.type = Type.NORMAL;
        }
    }

    @Override
    public void onCreateObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, Object parent)
    {

    }

    @Override
    public void onUpdateObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, Object parent)
    {

    }

    @Override
    public void onRemoveObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, Object parent)
    {
        database.getContext().deleteFile(generatePictureId());

        database.remove(dbInstance, new SQLQuery.Select(AccessDatabase.TABLE_DEVICECONNECTION)
                .setWhere(AccessDatabase.FIELD_DEVICECONNECTION_DEVICEID + "=?", id));

        List<TransferGroup.Assignee> assignees = database.castQuery(dbInstance, new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERASSIGNEE)
                .setWhere(AccessDatabase.FIELD_TRANSFERASSIGNEE_DEVICEID + "=?", id), TransferGroup.Assignee.class, null);

        // We are ensuring that the transfer group is still valid for other devices
        for (TransferGroup.Assignee assignee : assignees) {
            database.remove(dbInstance, assignee, this);

            try {
                TransferGroup transferGroup = new TransferGroup(assignee.groupId);
                database.reconstruct(dbInstance, transferGroup);

                List<TransferGroup.Assignee> relatedAssignees = database.castQuery(dbInstance, new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERASSIGNEE)
                        .setWhere(AccessDatabase.FIELD_TRANSFERASSIGNEE_GROUPID + "=?", String.valueOf(transferGroup.id)), TransferGroup.Assignee.class, null);

                if (relatedAssignees.size() == 0)
                    database.remove(dbInstance, transferGroup, this);
            } catch (Exception e) {

            }
        }
    }

    public enum Type {
        NORMAL,
        WEB
    }

    public static class Connection implements DatabaseObject<NetworkDevice>
    {
        public String adapterName;
        public String ipAddress;
        public String deviceId;
        public long lastCheckedDate;

        public Connection()
        {
        }

        public Connection(String adapterName, String ipAddress, String deviceId, long lastCheckedDate)
        {
            this.adapterName = adapterName;
            this.ipAddress = ipAddress;
            this.deviceId = deviceId;
            this.lastCheckedDate = lastCheckedDate;
        }

        public Connection(String deviceId, String adapterName)
        {
            this.deviceId = deviceId;
            this.adapterName = adapterName;
        }

        public Connection(TransferGroup.Assignee assignee)
        {
            this(assignee.deviceId, assignee.connectionAdapter);
        }

        public Connection(String ipAddress)
        {
            this.ipAddress = ipAddress;
        }

        public Connection(CursorItem item)
        {
            reconstruct(item);
        }

        @Override
        public SQLQuery.Select getWhere()
        {
            SQLQuery.Select select = new SQLQuery.Select(AccessDatabase.TABLE_DEVICECONNECTION);

            return ipAddress == null
                    ? select.setWhere(AccessDatabase.FIELD_DEVICECONNECTION_DEVICEID + "=? AND "
                    + AccessDatabase.FIELD_DEVICECONNECTION_ADAPTERNAME + "=?", deviceId, adapterName)
                    : select.setWhere(AccessDatabase.FIELD_DEVICECONNECTION_IPADDRESS + "=?", ipAddress);
        }

        @Override
        public ContentValues getValues()
        {
            ContentValues values = new ContentValues();

            values.put(AccessDatabase.FIELD_DEVICECONNECTION_DEVICEID, deviceId);
            values.put(AccessDatabase.FIELD_DEVICECONNECTION_ADAPTERNAME, adapterName);
            values.put(AccessDatabase.FIELD_DEVICECONNECTION_IPADDRESS, ipAddress);
            values.put(AccessDatabase.FIELD_DEVICECONNECTION_LASTCHECKEDDATE, lastCheckedDate);

            return values;
        }

        @Override
        public void reconstruct(CursorItem item)
        {
            this.adapterName = item.getString(AccessDatabase.FIELD_DEVICECONNECTION_ADAPTERNAME);
            this.ipAddress = item.getString(AccessDatabase.FIELD_DEVICECONNECTION_IPADDRESS);
            this.deviceId = item.getString(AccessDatabase.FIELD_DEVICECONNECTION_DEVICEID);
            this.lastCheckedDate = item.getLong(AccessDatabase.FIELD_DEVICECONNECTION_LASTCHECKEDDATE);
        }

        @Override
        public void onCreateObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, NetworkDevice parent)
        {

        }

        @Override
        public void onUpdateObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, NetworkDevice parent)
        {

        }

        @Override
        public void onRemoveObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, NetworkDevice parent)
        {

        }
    }
}
