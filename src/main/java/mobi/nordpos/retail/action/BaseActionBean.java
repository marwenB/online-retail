/**
 * Copyright (c) 2012-2015 Nord Trading Network.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package mobi.nordpos.retail.action;

import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import mobi.nordpos.dao.model.Application;
import mobi.nordpos.dao.ormlite.ApplicationPersist;
import mobi.nordpos.retail.ext.MobileActionBeanContext;
import mobi.nordpos.retail.ext.MyLocalePicker;
import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.controller.StripesFilter;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.ValidationErrors;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Andrey Svininykh <svininykh@gmail.com>
 */
public abstract class BaseActionBean implements ActionBean {

    private static final String WEB_APP_VERSION = "1.0.0-SNAPSHOT";
    private static final String DB_URL = "db.URL";
    private static final String DB_USER = "db.user";
    private static final String DB_PASSWORD = "db.password";
    private static final String DB_APP = "db.application.id";

    private MobileActionBeanContext context;
    private Application application;

    Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private final ApplicationPersist appPersist;

    public BaseActionBean() {
        appPersist = new ApplicationPersist();
    }

    @Override
    public MobileActionBeanContext getContext() {
        return this.context;
    }

    @Override
    public void setContext(ActionBeanContext actionBeanContext) {
        this.context = (MobileActionBeanContext) actionBeanContext;
    }

    @SuppressWarnings("unchecked")
    public String getLastUrl() {
        HttpServletRequest req = getContext().getRequest();
        StringBuilder sb = new StringBuilder();

        // Start with the URI and the path
        String uri = (String) req.getAttribute("javax.servlet.forward.request_uri");
        String path = (String) req.getAttribute("javax.servlet.forward.path_info");
        if (uri == null) {
            uri = req.getRequestURI();
            path = req.getPathInfo();
        }
        sb.append(uri);
        if (path != null) {
            sb.append(path);
        }

        // Now the request parameters
        sb.append('?');
        Map<String, String[]> map
                = new HashMap<String, String[]>(req.getParameterMap());

        // Remove previous locale parameter, if present.
        map.remove(MyLocalePicker.LOCALE);

        // Append the parameters to the URL
        for (String key : map.keySet()) {
            String[] values = map.get(key);
            for (String value : values) {
                sb.append(key).append('=').append(value).append('&');
            }
        }
        // Remove the last '&'
        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }

    public String getLocalizationKey(String key) {
        return StripesFilter.getConfiguration().getLocalizationBundleFactory()
                .getFormFieldBundle(getContext().getLocale()).getString(key);
    }

    public Application getApplication() {
        return application;
    }

    @ValidationMethod
    public void validateApplicationAvalaible(ValidationErrors errors) {
        try {
            appPersist.init(getDataBaseConnection());
            application = appPersist.read(getDataBaseApplication());
            if (application == null) {
                errors.add("application.id", new SimpleError(
                        getLocalizationKey("error.DatabaseNotSupportApplication"), getDataBaseApplication()));
            }
        } catch (SQLException ex) {
            getContext().getValidationErrors().addGlobalError(
                    new SimpleError(ex.getMessage()));
        }
    }

    public String getWebApplicationVersion() {
        return WEB_APP_VERSION;
    }

    protected ConnectionSource getDataBaseConnection() throws SQLException {
        return new JdbcConnectionSource(getDataBaseURL(), getDataBaseUser(), getDataBasePassword());
    }

    private String getDataBaseURL() {
        return getContext().getServletContext().getInitParameter(DB_URL);
    }

    private String getDataBaseUser() {
        return getContext().getServletContext().getInitParameter(DB_USER);
    }

    private String getDataBasePassword() {
        return getContext().getServletContext().getInitParameter(DB_PASSWORD);
    }

    public String getDataBaseApplication() {
        return getContext().getServletContext().getInitParameter(DB_APP);
    }

}
