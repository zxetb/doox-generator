/*
 * Copyright 2014 nqcx.org All right reserved. This software is the
 * confidential and proprietary information of nqcx.org ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with nqcx.org.
 */

package org.nqcx.cg.service.generate.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.nqcx.cg.common.util.CgFileUtils;
import org.nqcx.cg.entity.ws.enums.PType;
import org.nqcx.cg.provide.o.CgField;
import org.nqcx.cg.provide.o.Generate;
import org.nqcx.cg.service.generate.GenerateService;
import org.nqcx.commons3.lang.o.DTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * @author naqichuan Feb 9, 2014 2:18:27 AM
 */
@Service
public class GenerateServiceImpl implements GenerateService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static String JAVA_PATH = "src/main/java/";
    private static String JAVA_EXT_NAME = ".java";
    private static String XML_EXT_NAME = ".xml";

    private final static String BO_TXT_TEMPLATE_NAME = "bo.txt";
    private final static String O_TXT_TEMPLATE_NAME = "o.txt";
    private final static String PROVIDE_TXT_TEMPLATE_NAME = "provide.txt";
    private final static String PO_TXT_TEMPLATE_NAME = "po.txt";
    private final static String MAPPER_TXT_TEMPLATE_NAME = "mapper.txt";
    private final static String MAPPERXML_TXT_TEMPLATE_NAME = "mapper_xml.txt";
    private final static String JPA_TXT_TEMPLATE_NAME = "jpa.txt";
    private final static String DAO_TXT_TEMPLATE_NAME = "dao.txt";
    private final static String DAOIMPL_TXT_TEMPLATE_NAME = "daoimpl.txt";

    //    private final static String ENTITY_TEMPLATE_NAME = "entity.vm";
//    private final static String MAPPER_JAVA_TEMPLATE_NAME = "mapper_java.vm";
//    private final static String MAPPER_XML_TEMPLATE_NAME = "mapper_xml.vm";
    private final static String SERVICE_INTERFACE_TEMPLATE_NAME = "service_interface.vm";
    private final static String SERVICE_IMPLEMENTS_TEMPLATE_NAME = "service_implements.vm";

    private static String P_PROVIDE_PATH_KEY = "provideModule";
    private static String P_DAO_PATH_KEY = "daoModule";
    private static String P_SERVICE_PATH_KEY = "serviceModule";
    private static String P_WEB_PATH_KEY = "webModule";

    private final static Map<String, String> classMapping = new HashMap<>();


    @Autowired
    @Qualifier("workspacePath")
    private String workspacePath;
    @Autowired
    @Qualifier("workspaceAuthor")
    private String workspaceAuthor;

    static {
        classMapping.put("Serializable", "java.io.Serializable");
        classMapping.put("Date", "java.util.Date");
        classMapping.put("Integer", "java.lang.Integer");
        classMapping.put("Long", "java.lang.Long");
        classMapping.put("Entity", "javax.persistence.Entity");
        classMapping.put("Table", "javax.persistence.Table");
    }

    @Override
    public DTO generate(Generate g, String tableName, String pName, PType pType, String pPath, String pPackage,
                        String entityName,
                        String entityProjectName, String entityProjectPackage,
                        String mapperProjectName,
                        String mapperProjectPackage,
                        String serviceProjectName, String serviceProjectPackage,
                        String[] tableColumns, String[] provideField, String[] provideType,
                        String mapperInterface, String serviceInterface, String serviceImplement) {

        if (g == null)
            return new DTO(false).putResult("100", "生成代码失败！");

        if (!pPathExist(workspacePath, g.getpPath()))
            return new DTO(false).putResult("101", "工程路径不存在");

        Map<String, String> pathMap = this.getPathString(workspacePath, pPath, g.getProvideModule(),
                g.getDaoModule(), g.getServiceModule(), g.getWebModule());

        classMapping.put(g.getProvideBO(), g.getProvideBOPackage() + "." + g.getProvideBO());
        classMapping.put(g.getProvideO(), g.getProvideOPackage() + "." + g.getProvideO());
        classMapping.put(g.getProvideProvide(), g.getProvideProvidePackage() + "." + g.getProvideProvide());

        String providePath = pathMap.get(P_PROVIDE_PATH_KEY);
        if (g.getProvide_().isTrue()) {
            this.generateProvide(providePath, g.getProvideBOPackage(), g.getProvideBO(),
                    g.getProvideOPackage(), g.getProvideO(),
                    g.getProvideProvidePackage(), g.getProvideProvide(),
                    g.getTableColumns(), g.getProvideFields(), g.getProvideTypes());
        }

        classMapping.put(g.getDaoPO(), g.getDaoPOPackage() + "." + g.getDaoPO());
        classMapping.put(g.getDaoMapper(), g.getDaoMapperPackage() + "." + g.getDaoMapper());
        classMapping.put(g.getDaoJpa(), g.getDaoJpaPackage() + "." + g.getDaoJpa());
        classMapping.put(g.getDaoDAO(), g.getDaoDaoPackage() + "." + g.getDaoDAO());
        classMapping.put(g.getDaoDAOImpl(), g.getDaoDaoPackage() + ".impl." + g.getDaoDAOImpl());

        String daoPath = pathMap.get(P_DAO_PATH_KEY);
        if (g.getDao_().isTrue()) {
            this.generateDao(daoPath, g.getDaoPOPackage(), g.getDaoPO(),
                    g.getDaoMapperPackage(), g.getDaoMapper(),
                    g.getDaoJpaPackage(), g.getDaoJpa(),
                    g.getDaoDaoPackage(), g.getDaoDAO(), g.getDaoDaoPackage() + ".impl", g.getDaoDAOImpl(),
                    g.getTableName(), g.getTableColumns(), g.getProvideFields(), g.getProvideTypes(),
                    g.getProvideBO());
        }
//
//        String daoInterface = mapperInterface;
//        String daoProjectPackage = mapperProjectPackage;
//
//        String servicePath = pathMap.get(P_SERVICE_PATH_KEY);
//        if ((servicePath != null || (PType.MULTIPLE == pType && isNotBlank(serviceProjectName)))
//                && isNotBlank(serviceInterface) && isNotBlank(serviceImplement) && isNotBlank(serviceProjectPackage)) {
//            this.generateService(serviceProjectName, servicePath, serviceInterface, serviceImplement,
//                    serviceProjectPackage, daoInterface, daoProjectPackage);
//        }

        return new DTO(true);
    }

    /**
     * @param wsPath
     * @param pPath
     * @return
     */
    private boolean pPathExist(String wsPath, String pPath) {
        File file = new File(wsPath + pPath);
        if (!file.exists())
            return false;
        return true;
    }

    /**
     * @param wsPath
     * @param pPath
     * @param provideModuleName
     * @param daoModuleName
     * @param serviceModuleName
     * @param webModuleName
     * @return
     */
    private Map<String, String> getPathString(String wsPath, String pPath,
                                              String provideModuleName,
                                              String daoModuleName,
                                              String serviceModuleName,
                                              String webModuleName) {
        Map<String, String> map = new HashMap<String, String>();

        String provideModulePath = wsPath + pPath + provideModuleName;
        if (new File(provideModulePath).exists())
            map.put(P_PROVIDE_PATH_KEY, provideModulePath);

        String daoModulePath = wsPath + pPath + daoModuleName;
        if (new File(daoModulePath).exists())
            map.put(P_DAO_PATH_KEY, daoModulePath);

        String serviceModulePath = wsPath + pPath + webModuleName;
        if ((new File(serviceModulePath)).exists())
            map.put(P_SERVICE_PATH_KEY, serviceModulePath);

        String webModulePath = wsPath + pPath + serviceModuleName;
        if ((new File(webModulePath)).exists())
            map.put(P_WEB_PATH_KEY, webModulePath);


        return map;
    }

    /**
     * @param imports
     * @param className
     */
    private void mappingImport(Set<String> imports, String className) {
        if (imports == null || className == null)
            return;

        String name = classMapping.get(className);
        if (name != null && name.length() > 0)
            imports.add(name);
    }

    /**
     * @param providePath
     * @param boPackage
     * @param boName
     * @param oPackage
     * @param oName
     * @param providePackage
     * @param provideName
     * @param fields
     * @param types
     */
    private void generateProvide(String providePath, String boPackage, String boName,
                                 String oPackage, String oName,
                                 String providePackage, String provideName,
                                 String[] tableColumns,
                                 String[] fields,
                                 String[] types) {

        Context cxt = new Context();
        Set<String> imports = new HashSet<>();

        // bo
        mappingImport(imports, "Serializable");

        cxt.setVariable("author", workspaceAuthor);
        cxt.setVariable("date", new Date());
        cxt.setVariable("package", boPackage);
        cxt.setVariable("imports", imports);
        cxt.setVariable("name", boName);

        if (fields != null && types != null && tableColumns != null
                && fields.length == types.length && fields.length == tableColumns.length) {
            // bo field
            List<String> boFields = new ArrayList<>();
            // getter & setter
            List<CgField> boGetterAndSetters = new ArrayList<>();
            for (int i = 0; i < fields.length; i++) {
                if (tableColumns[i].contains("_create") || tableColumns[i].contains("_modify"))
                    continue;

                mappingImport(imports, types[i]);

                boFields.add(String.format("private %s %s;", types[i], fields[i]));

                CgField field = new CgField();
                field.setType(types[i]);
                field.setField(fields[i]);
                field.setName(StringUtils.capitalize(fields[i]));

                boGetterAndSetters.add(field);
            }
            cxt.setVariable("boFields", boFields);
            cxt.setVariable("boGetterAndSetter", boGetterAndSetters);
        }

        this.writeFile(providePath + "/" + JAVA_PATH + boPackage.replace('.', '/'), boName, JAVA_EXT_NAME,
                process(BO_TXT_TEMPLATE_NAME, cxt));

        // o
        cxt.clearVariables();
        imports.clear();
        mappingImport(imports, boName);

        cxt.setVariable("author", workspaceAuthor);
        cxt.setVariable("date", new Date());
        cxt.setVariable("package", oPackage);
        cxt.setVariable("imports", imports);
        cxt.setVariable("name", oName);

        cxt.setVariable("boName", boName);

        this.writeFile(providePath + "/" + JAVA_PATH + oPackage.replace('.', '/'), oName, JAVA_EXT_NAME,
                process(O_TXT_TEMPLATE_NAME, cxt));

        // provide
        cxt.clearVariables();
        imports.clear();

        cxt.setVariable("author", workspaceAuthor);
        cxt.setVariable("date", new Date());
        cxt.setVariable("package", providePackage);
        cxt.setVariable("imports", imports);
        cxt.setVariable("name", provideName);

        this.writeFile(providePath + "/" + JAVA_PATH + providePackage.replace('.', '/'), provideName, JAVA_EXT_NAME,
                process(PROVIDE_TXT_TEMPLATE_NAME, cxt));
    }

    /**
     * @param daoPath
     * @param poPackage
     * @param po
     * @param mapperPackage
     * @param mapper
     * @param jpaPackage
     * @param jpa
     * @param daoPackage
     * @param dao
     * @param daoImplPackage
     * @param daoImpl
     * @param tableColumns
     * @param fields
     * @param types
     */
    private void generateDao(String daoPath, String poPackage, String po,
                             String mapperPackage, String mapper,
                             String jpaPackage, String jpa,
                             String daoPackage, String dao, String daoImplPackage, String daoImpl,
                             String tableName,
                             String[] tableColumns,
                             String[] fields,
                             String[] types,
                             String boName) {

        Context cxt = new Context();
        Set<String> imports = new HashSet<>();

        // po
        mappingImport(imports, boName);
        mappingImport(imports, "Entity");
        mappingImport(imports, "Table");

        cxt.setVariable("author", workspaceAuthor);
        cxt.setVariable("date", new Date());
        cxt.setVariable("package", poPackage);
        cxt.setVariable("imports", imports);
        cxt.setVariable("name", po);

        cxt.setVariable("boName", boName);
        cxt.setVariable("tableName", tableName);

        if (fields != null && types != null && tableColumns != null
                && fields.length == types.length && fields.length == tableColumns.length) {
            // bo field
            List<String> poFields = new ArrayList<>();
            // po getter
            List<CgField> poGetter = new ArrayList<>();
            // getter & setter
            List<CgField> poGetterAndSetters = new ArrayList<>();
            for (int i = 0; i < fields.length; i++) {

                mappingImport(imports, types[i]);

                CgField field = new CgField();
                field.setType(types[i]);
                field.setField(fields[i]);
                field.setName(StringUtils.capitalize(fields[i]));

                if (tableColumns[i].contains("_create") || tableColumns[i].contains("_modify")) {
                    poFields.add(String.format("private %s %s;", types[i], fields[i]));
                    cxt.setVariable("poFields", poFields);

                    cxt.setVariable("poGetterAndSetter", poGetterAndSetters);

                    poGetterAndSetters.add(field);
                } else {
                    cxt.setVariable("poGetter", poGetter);

                    poGetter.add(field);
                }
            }
        }


        this.writeFile(daoPath + "/" + JAVA_PATH + poPackage.replace('.', '/'),
                po, JAVA_EXT_NAME,
                process(PO_TXT_TEMPLATE_NAME, cxt));

        // mapper
        cxt.clearVariables();
        imports.clear();

        cxt.setVariable("author", workspaceAuthor);
        cxt.setVariable("date", new Date());
        cxt.setVariable("package", mapperPackage);
        cxt.setVariable("imports", imports);
        cxt.setVariable("name", mapper);

        cxt.setVariable("poName", po);

        this.writeFile(daoPath + "/" + JAVA_PATH + mapperPackage.replace('.', '/'),
                mapper, JAVA_EXT_NAME,
                process(MAPPER_TXT_TEMPLATE_NAME, cxt));

        // mapper xml
        this.writeFile(daoPath + "/" + JAVA_PATH + mapperPackage.replace('.', '/'),
                mapper, XML_EXT_NAME,
                process(MAPPERXML_TXT_TEMPLATE_NAME, cxt));


        // jpa
        cxt.clearVariables();
        imports.clear();

        cxt.setVariable("author", workspaceAuthor);
        cxt.setVariable("date", new Date());
        cxt.setVariable("package", jpaPackage);
        cxt.setVariable("imports", imports);
        cxt.setVariable("name", jpa);

        this.writeFile(daoPath + "/" + JAVA_PATH + jpaPackage.replace('.', '/'),
                jpa, JAVA_EXT_NAME,
                process(JPA_TXT_TEMPLATE_NAME, cxt));

        // dao
        cxt.clearVariables();
        imports.clear();

        cxt.setVariable("author", workspaceAuthor);
        cxt.setVariable("date", new Date());
        cxt.setVariable("package", daoPackage);
        cxt.setVariable("imports", imports);
        cxt.setVariable("name", dao);

        this.writeFile(daoPath + "/" + JAVA_PATH + daoPackage.replace('.', '/'),
                dao, JAVA_EXT_NAME,
                process(DAO_TXT_TEMPLATE_NAME, cxt));

        // dao impl
        cxt.clearVariables();
        imports.clear();

        cxt.setVariable("author", workspaceAuthor);
        cxt.setVariable("date", new Date());
        cxt.setVariable("package", daoImplPackage);
        cxt.setVariable("imports", imports);
        cxt.setVariable("name", daoImpl);

        cxt.setVariable("daoName", dao);

        mappingImport(imports, dao);

        this.writeFile(daoPath + "/" + JAVA_PATH + daoImplPackage.replace('.', '/'),
                daoImpl, JAVA_EXT_NAME,
                process(DAOIMPL_TXT_TEMPLATE_NAME, cxt));

//        Map<String, Object> model = new HashMap<String, Object>();
//        model.put("date", new Date());
//        model.put("mapperName", mapperName);
//        model.put("mapperPackage", mapperPackage);
//        model.put("tableName", tableName);
//        model.put("entityName", entityName);
//
//        List<String> resultMap = new ArrayList<String>();
//        List<String> entityFields = new ArrayList<String>();
//        String tableColumnsStr = new String();
//        String idColumn = new String();
//        String idField = new String();
//        String idFieldName = new String();
//
//        if (tableColumns != null && entityField != null && tableColumns.length == entityField.length)
//            for (int i = 0; i < entityField.length; i++) {
//                if (i == 0) {
//                    idColumn = tableColumns[i];
//                    idFieldName = entityField[i];
//                    idField = "#{" + entityField[i] + "}";
//                }
//
//                if ("id".equals(entityField[i])) {
//                    resultMap.add("<id property=\"" + entityField[i] + "\" column=\"" + tableColumns[i] + "\" />");
//
//                    entityFields.add("NULL");
//
//                    idColumn = tableColumns[i];
//                    idFieldName = entityField[i];
//                    idField = "#{" + entityField[i] + "}";
//                } else {
//                    resultMap.add("<result property=\"" + entityField[i] + "\" column=\"" + tableColumns[i] + "\" />");
//
//                    entityFields.add("#{" + entityField[i] + "}");
//                }
//
//                if (tableColumnsStr.length() == 0)
//                    tableColumnsStr += tableColumns[i];
//                else
//                    tableColumnsStr += ", " + tableColumns[i];
//
//            }
//        model.put("idColumn", idColumn);
//        model.put("idField", idField);
//        model.put("idFieldName", idFieldName);
//        model.put("tableColumnsStr", tableColumnsStr);
//        model.put("resultMap", resultMap);
//        model.put("entityFields", entityFields);
//        model.put("tableColumns", tableColumns);
//
//        this.writeFile(
//                CgFileUtils.formatPath(mapperPath + "/" + JAVA_PATH + mapperPackage.replace('.', '/'), false, false),
//                mapperName, JAVA_EXT_NAME, mergeTemplateIntoString(MAPPER_JAVA_TEMPLATE_NAME, model));
//
//        this.writeFile(
//                CgFileUtils.formatPath(mapperPath + "/" + JAVA_PATH + mapperPackage.replace('.', '/'), false, false),
//                mapperName, XML_EXT_NAME, mergeTemplateIntoString(MAPPER_XML_TEMPLATE_NAME, model));
    }

    // /**
    // * @param managerProjectName
    // * @param managerPath
    // * @param managerInterface
    // * @param managerImplement
    // * // * @param managerProjectPackage
    // * @param mapperInterface
    // * @param mapperProjectPackage
    // */
    // private void generateManager(String managerProjectName, String managerPath, String managerInterface,
    // String managerImplement,/* String managerProjectPackage, */String mapperInterface,
    // String mapperProjectPackage) {
    //
    // Map<String, Object> modelInterface = new HashMap<String, Object>();
    // modelInterface.put("date", new Date());
    // modelInterface.put("managerInterface", managerInterface);
    // modelInterface.put("managerProjectPackage", managerProjectPackage);
    //
    // this.writeFile(CgFileUtils.formatPath(managerPath + "/" + JAVA_PATH + managerProjectPackage.replace('.', '/'),
    // false, false), managerInterface + JAVA_EXT_NAME,
    // mergeTemplateIntoString(MANAGERR_INTERFACE_TEMPLATE_NAME, modelInterface));
    //
    // Map<String, Object> modelImplement = new HashMap<String, Object>();
    // modelImplement.put("date", new Date());
    // modelImplement.put("managerInterface", managerInterface);
    // modelImplement.put("managerProjectPackage", managerProjectPackage);
    // modelImplement.put("managerImplements", managerImplement);
    // modelImplement.put("managerProjectPackage1", managerProjectPackage + ".impl");
    // modelImplement.put("mapperInterface", mapperInterface);
    // modelImplement.put("mapperInterface1", StringUtils.uncapitalize(mapperInterface));
    // modelImplement.put("mapperProjectPackage", mapperProjectPackage);
    //
    // this.writeFile(CgFileUtils.formatPath(
    // managerPath + "/" + JAVA_PATH + (managerProjectPackage + ".impl").replace('.', '/'), false, false),
    // managerImplement + JAVA_EXT_NAME,
    // mergeTemplateIntoString(MANAGERR_IMPLEMENTS_TEMPLATE_NAME, modelImplement));
    // }

    /**
     * @param serviceProjectName
     * @param servicePath
     * @param serviceInterface
     * @param serviceImplement
     * @param serviceProjectPackage
     * @param daoInterface
     * @param daoProjectPackage
     */
    private void generateService(String serviceProjectName, String servicePath, String serviceInterface,
                                 String serviceImplement, String serviceProjectPackage, String daoInterface, String daoProjectPackage) {

        Map<String, Object> modelInterface = new HashMap<String, Object>();
        modelInterface.put("date", new Date());
        modelInterface.put("serviceInterface", serviceInterface);
        modelInterface.put("serviceProjectPackage", serviceProjectPackage);

        this.writeFile(CgFileUtils.formatPath(servicePath + "/" + JAVA_PATH + serviceProjectPackage.replace('.', '/'),
                false, false), serviceInterface, JAVA_EXT_NAME,
                mergeTemplateIntoString(SERVICE_INTERFACE_TEMPLATE_NAME, modelInterface));

        Map<String, Object> modelImplement = new HashMap<String, Object>();
        modelImplement.put("date", new Date());
        modelImplement.put("serviceInterface", serviceInterface);
        modelImplement.put("serviceImplements", serviceImplement);
        modelImplement.put("serviceProjectPackage", serviceProjectPackage);
        modelImplement.put("serviceProjectPackage1", serviceProjectPackage + ".impl");
        modelImplement.put("daoInterface", daoInterface);
        modelImplement.put("daoInterface1", StringUtils.uncapitalize(daoInterface));
        modelImplement.put("daoProjectPackage", daoProjectPackage);

        this.writeFile(CgFileUtils.formatPath(
                servicePath + "/" + JAVA_PATH + (serviceProjectPackage + ".impl").replace('.', '/'), false, false),
                serviceImplement, JAVA_EXT_NAME,
                mergeTemplateIntoString(SERVICE_IMPLEMENTS_TEMPLATE_NAME, modelImplement));
    }

    /**
     * @return
     * @throws Exception
     */
    private VelocityEngine velocityEngine() {
        Properties properties = new Properties();
        properties.setProperty("input.encoding", "UTF-8");
        properties.setProperty("output.encoding", "UTF-8");

        properties.setProperty("resource.loader", "file");
        properties.setProperty("file.resource.loader.cache", "true");
        properties.setProperty("file.resource.loader.path", "classpath:/template/");

        properties.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        VelocityEngine velocityEngine = new VelocityEngine(properties);
        return velocityEngine;
    }

    /**
     * @param templateLocation
     * @param model
     * @return
     */
    private String mergeTemplateIntoString(String templateLocation, Map<String, Object> model) {
        VelocityEngine velocityEngine = velocityEngine();

        VelocityContext context = new VelocityContext(model);

        StringWriter stringWriter = new StringWriter();
        velocityEngine.mergeTemplate(templateLocation, "UTF-8", context, stringWriter);

        return stringWriter.toString();
    }

    private String process(String template, IContext context) {
        return templateEngine().process(template, context);
    }


    public TemplateEngine templateEngine() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();

        templateResolver.setTemplateMode(TemplateMode.TEXT);
        templateResolver.setPrefix("/template/");
        templateResolver.setSuffix(".txt");
        templateResolver.setCacheTTLMs(Long.valueOf(3600000L));
        templateResolver.setCacheable(false);

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        return templateEngine;
    }

    /**
     * @param path
     * @param content
     */
    private void writeFile(String path, String name, String ext, String content) {
        if (isBlank(path))
            return;
        if (content == null)
            content = "";

        File pathFile = new File(path);
        if (!pathFile.exists())
            pathFile.mkdirs();

        String fileName = path + "/" + name + ext;
        File file = new File(fileName);
        if (file.exists())
            fileName = path + "/" + name + ext;
        // TODO sssssssssssssssssss
//            fileName = path + "/" + name + "_" + ext;

        FileWriter fw = null;
        try {
            fw = new FileWriter(fileName);
            fw.write(content, 0, content.length());
            fw.flush();
        } catch (IOException e) {
            logger.error("", e);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    logger.error("", e);
                }
            }
        }
    }

    /**
     * @param str
     * @return
     */
    private boolean isNotBlank(String str) {
        return StringUtils.isNotBlank(str);
    }

    /**
     * @param str
     * @return
     */
    private boolean isBlank(String str) {
        return StringUtils.isBlank(str);
    }
}
