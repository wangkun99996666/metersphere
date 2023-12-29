package io.metersphere.functional.service;

import io.metersphere.dto.TestCaseProviderDTO;
import io.metersphere.functional.constants.AssociateCaseType;
import io.metersphere.functional.domain.FunctionalCaseTest;
import io.metersphere.functional.domain.FunctionalCaseTestExample;
import io.metersphere.functional.mapper.ExtFunctionalCaseTestMapper;
import io.metersphere.functional.mapper.FunctionalCaseTestMapper;
import io.metersphere.functional.request.FunctionalTestCaseDisassociateRequest;
import io.metersphere.provider.BaseAssociateApiProvider;
import io.metersphere.request.ApiModuleProviderRequest;
import io.metersphere.request.TestCasePageProviderRequest;
import io.metersphere.request.AssociateOtherCaseRequest;
import io.metersphere.sdk.util.LogUtils;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;
import org.redisson.api.IdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * @author guoyuqi
 * 功能用例关联其他用例服务实现类
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class FunctionalTestCaseService {

    @Resource
    private BaseAssociateApiProvider provider;

    @Resource
    SqlSessionFactory sqlSessionFactory;

    @Resource
    private ExtFunctionalCaseTestMapper extFunctionalCaseTestMapper;

    @Resource
    private FunctionalCaseTestMapper functionalCaseTestMapper;

    /**
     * 获取功能用例未关联的接口用例列表
     *
     * @param request request
     * @return List<ApiTestCaseProviderDTO>
     */
    public List<TestCaseProviderDTO> page(TestCasePageProviderRequest request) {
        return provider.getApiTestCaseList("functional_case_test", "case_id", "source_id", request);
    }

    /**
     * 根据接口用例的搜索条件获取符合条件的接口定义的模块统计数量
     *
     * @param request 接口用例高级搜索条件
     * @param deleted 接口定义是否删除
     * @return 接口模块统计数量
     */
    public Map<String, Long> moduleCount(ApiModuleProviderRequest request, boolean deleted) {
        return provider.moduleCount("functional_case_test", "case_id", "source_id", request, deleted);

    }

    /**
     * 关联其他用例
     *
     * @param request request
     * @param deleted 接口定义是否删除
     */
    public void associateCase(AssociateOtherCaseRequest request, boolean deleted, String userId) {

        switch (request.getSourceType()) {
            case AssociateCaseType.API -> associateApi(request, deleted, userId);
            case AssociateCaseType.SCENARIO -> associateScenario(request, deleted, userId);
        }


    }

    private void associateScenario(AssociateOtherCaseRequest request, boolean deleted, String userId) {
        LogUtils.info("关联场景");
    }

    private void associateApi(AssociateOtherCaseRequest request, boolean deleted, String userId) {
        List<String> selectIds = provider.getSelectIds(request, deleted);
        if (CollectionUtils.isEmpty(selectIds)) {
            return;
        }
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        FunctionalCaseTestMapper caseTestMapper = sqlSession.getMapper(FunctionalCaseTestMapper.class);
        for (String selectId : selectIds) {
            FunctionalCaseTest functionalCaseTest = new FunctionalCaseTest();
            functionalCaseTest.setCaseId(request.getSourceId());
            functionalCaseTest.setProjectId(request.getProjectId());
            functionalCaseTest.setSourceId(selectId);
            functionalCaseTest.setSourceType(request.getSourceType());
            functionalCaseTest.setId(IdGenerator.random().generateId());
            functionalCaseTest.setCreateUser(userId);
            functionalCaseTest.setCreateTime(System.currentTimeMillis());
            functionalCaseTest.setUpdateUser(userId);
            functionalCaseTest.setUpdateTime(System.currentTimeMillis());
            caseTestMapper.insert(functionalCaseTest);
        }
        sqlSession.flushStatements();
        SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);
    }

    /**
     * 取消关联其他用例
     *
     * @param request request
     */
    public void disassociateCase(FunctionalTestCaseDisassociateRequest request) {
        List<String> functionalTestCaseIds = doSelectIds(request);
        FunctionalCaseTestExample functionalCaseTestExample = new FunctionalCaseTestExample();
        if (CollectionUtils.isNotEmpty(functionalTestCaseIds)) {
            functionalCaseTestExample.createCriteria().andIdIn(functionalTestCaseIds);
            functionalCaseTestMapper.deleteByExample(functionalCaseTestExample);
        }
    }


    public List<String> doSelectIds(FunctionalTestCaseDisassociateRequest request) {
        if (request.isSelectAll()) {
            List<String> ids = extFunctionalCaseTestMapper.getIds(request);
            if (org.apache.commons.collections.CollectionUtils.isNotEmpty(request.getExcludeIds())) {
                ids.removeAll(request.getExcludeIds());
            }
            return ids;
        } else {
            return request.getSelectIds();
        }
    }
}
