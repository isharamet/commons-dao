/*
 * Copyright 2016 EPAM Systems
 * 
 * 
 * This file is part of EPAM Report Portal.
 * https://github.com/epam/ReportPortal
 * 
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */ 

package com.epam.ta.reportportal.database.dao;

import com.epam.ta.reportportal.commons.Constants;
import com.epam.ta.reportportal.commons.EntityUtils;
import com.epam.ta.reportportal.database.BinaryData;
import com.epam.ta.reportportal.database.DataStorage;
import com.epam.ta.reportportal.database.entity.project.EntryType;
import com.epam.ta.reportportal.database.entity.user.User;
import com.mongodb.gridfs.GridFSDBFile;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import static com.epam.ta.reportportal.database.entity.user.User.IS_EXPIRED;
import static com.epam.ta.reportportal.database.entity.user.User.LOGIN;
import static com.epam.ta.reportportal.database.entity.user.User.MetaInfo;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.core.query.Update.update;

/**
 * Default Implementation of {@link UserRepositoryCustom}
 *
 * @author Andrei Varabyeu
 */
class UserRepositoryCustomImpl implements UserRepositoryCustom {

    @Autowired
    private MongoOperations mongoOperations;

    @Autowired
    private DataStorage dataStorage;

    @Override
    public void expireUsersLoggedOlderThan(Date lastLogin) {
        mongoOperations.updateMulti(query(Criteria.where(MetaInfo.LAST_LOGIN_PATH).lt(lastLogin)),
                update(IS_EXPIRED, true),
                User.class);
    }

    @Override
    public Page<User> findByTypeAndLastSynchronizedBefore(EntryType type, Date lastSynchronized, Pageable pageable) {
        Query q = query(Criteria.where("type").is(type).and(MetaInfo.SYNCHRONIZATION_DATE).lt(lastSynchronized))
                .with(pageable);
        long count = mongoOperations.count(q, User.class);
        return new PageImpl<>(mongoOperations.find(q, User.class), pageable, count);

    }

    @Override
    public String saveUserPhoto(String login, BinaryData binaryData) {
        /*
		 * Clean out-dated user photo (if exists) and create newest one
		 */
        StringBuilder builder = new StringBuilder(UserUtils.PHOTO_FILENAME_PREFIX).append(login);
        if (login.equalsIgnoreCase(Constants.NONAME_USER.toString())) {
            GridFSDBFile file = dataStorage.findGridFSFileByFilename(builder.toString());
            if (null != file)
                dataStorage.deleteData(file.getId().toString());
        } else {
            BinaryData previous = dataStorage.findByFilename(builder.toString());
            if (previous != null)
                this.deleteUserPhoto(login);
        }
        return dataStorage.saveData(binaryData, UserUtils.photoFilename(login));
    }

    @Override
    public void deleteUserPhoto(String login) {
        Query q = query(Criteria.where("login").is(login));
        User user = mongoOperations.findOne(q, User.class);
        if (null != user && !StringUtils.isEmpty(user.getPhotoId())) {
            dataStorage.deleteData(user.getPhotoId());
            user.setPhotoId(null);
            mongoOperations.save(user);
        }
    }

    @Override
    public void deleteUserPhotoById(String photoId) {
        if (!StringUtils.isEmpty(photoId)) {
            dataStorage.deleteData(photoId);
        }
    }

    @Override
    public BinaryData findUserPhoto(String login) {
        BinaryData photo = null;
        Query q = query(Criteria.where("login").is(login));
        q.fields().include("photoId");
        User user = mongoOperations.findOne(q, User.class);
        if (user != null && user.getPhotoId() != null)
            photo = dataStorage.fetchData(user.getPhotoId());
        if (null == photo) {
            // Get default photo avatar (batman)
            photo = dataStorage.findByFilename(UserUtils.photoFilename(Constants.NONAME_USER.toString()));
        }
        return photo;
    }

    @Override
    public List<User> findAllPhotos() {
        Query q = new Query();
        q.fields().include("_id");
        q.fields().include("photoId");
        return mongoOperations.find(q, User.class);
    }

    @Override
    public User findByEmail(String email) {
        final Query query = query(Criteria.where("email").is(EntityUtils.normalizeUsername(email)));
        return mongoOperations.findOne(query, User.class);
    }

    @Override
    public Page<User> findByLoginNameOrEmail(String term, Pageable pageable) {
        final String regex = "(?i).*" + Pattern.quote(term.toLowerCase()) + ".*";
        Criteria email = Criteria.where("email").regex(regex);
        Criteria login = Criteria.where(LOGIN).regex(regex);
        Criteria fullName = Criteria.where("fullName").regex(regex);
        Criteria criteria = new Criteria().orOperator(email, login, fullName);
        Query query = query(criteria).with(pageable);
        List<User> users = mongoOperations.find(query, User.class);
        return new PageImpl<>(users, pageable, mongoOperations.count(query, User.class));
    }

    @Override
    public void updateLastLoginDate(String user, Date date) {
        mongoOperations
                .updateFirst(query(Criteria.where("_id").is(user)), update("metaInfo.lastLogin", date),
                        User.class);

    }
}