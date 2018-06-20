package cn.mldn.util.servlet;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import cn.mldn.util.ResourceUtil;
import cn.mldn.util.action.ActionResourceUtil;
import cn.mldn.util.web.ActionObjectUtil;
import cn.mldn.util.web.ParameterUtil;
import cn.mldn.util.web.ParameterValidatorUtil;
import cn.mldn.util.web.RequestUriUtil;
import cn.mldn.util.web.ServletObjectUtil;
@SuppressWarnings("serial")
public class DispatcherServlet extends HttpServlet {
	// 定义一个集合，该集合保存路径以及对应的Class关系，所有的信息通过action.properties读取
	private Map<String, Class<?>> actionClassMap = new HashMap<String, Class<?>>();
/**
 * init 初始化 方法
 * ）config：javax.servlet.ServletConfig的实例，
 * 该实例代表JSP的配置信息，常用的方法有getInitparameter(String paramName)
 * 及getInitParametername()等方法。
 * 事实上，JSP页面通常无需配置，也就不存在配置信息。因此该对象更多地在Servlet中有效。
 */
	@Override
	public void init(ServletConfig config) throws ServletException {
		ServletObjectUtil.setAppliaction(config.getServletContext());
		ServletObjectUtil.setConfig(config);
		String baseName = config.getInitParameter("actionBaseName");
		String basePath = config.getServletContext().getRealPath("/") + "WEB-INF" + File.separator + "classes"
				+ File.separator + baseName.replaceAll("\\.", "/") + ".properties";
		//把properties资源文件中读取路径
		Properties pro = new Properties(); // 通过资源文件类进行加载     用io方法取得之源文件 并建立资源文件对象
		//给import java.util.Properties;  中的  properties实例化
		try {
			pro.load(new FileInputStream(basePath));
			//FileInputStream可以从文件系统中的某个文件中获得输入字节
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (pro.size() > 0) { // 现在有数据被读取进来了，意味可以将资源信息变为Map集合
			Iterator<Entry<Object, Object>> iter = pro.entrySet().iterator();
			//因为iterator 不能直接对map进行输出 所以这里用entry 方法对map集合进行输出并把类行设定为Object 
			try {
				while (iter.hasNext()) {
					Entry<Object, Object> me = iter.next();
					// 将所有的路径与程序类型保存在了Map集合之中，如果此时出现有类名称的错误则直接报错
					this.actionClassMap.put(me.getKey().toString(), Class.forName(me.getValue().toString()));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException { // 每一个用户访问的时候都是一个独立的线程
		if (request.getServletContext().getAttribute("basePath") == null) {
			String basePath = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
				+ request.getContextPath() ;
			request.getServletContext().setAttribute("basePath", basePath + "/");
			request.getServletContext().setAttribute("basePath2", basePath);
		}
		
		ServletObjectUtil.setRequest(request);
		ServletObjectUtil.setResponse(response);
		ServletObjectUtil.setParam(new ParameterUtil(request, "/tmp"));
		String dispatcherPath = null ; // 定义要跳转的处理路径
		String actionResult[] = RequestUriUtil.splitUri(request);
		String validateRule = null ;	// 获取验证规则
		try {	// 如果出现了错误，则表示验证规则可能不存在，或者本身路径有问题
			ResourceUtil resUtil = new ResourceUtil("cn.mldn.resource.validation") ;
			validateRule = resUtil.get(actionResult[0] + "!" + actionResult[1]) ;	// 根据路径获取规则
		} catch (Exception e) {}
		boolean flag = validateRule == null ;	// 定义个判断的标志位
		if (validateRule != null) {	// 现在有规则，有规则就需要规则的验证处理
			ParameterValidatorUtil pvu = new ParameterValidatorUtil(validateRule) ;
			if (!pvu.validate()) {	// 没有通过验证，需要将所有的错误提示信息发送给客户端
				request.setAttribute("errors", pvu.getErrors()); // 保存所有的错误提示信息
				// 出现了错误之后应该进行一个路径的跳转处理操作，所有的跳转路径可以直接在page.properties中定义
				dispatcherPath = ActionResourceUtil.getPage(actionResult[0] + "!" + actionResult[1] + ".errorPage") ;
				if (dispatcherPath == null) {	// 可能没有为单独的一个页面配置错误页
					dispatcherPath = ActionResourceUtil.getPage("error.page") ;	// 使用公共错误页
				}
				flag = false ; 
			} else {
				flag = true ;
			}
		} 
		if (flag) 
			try { // 在Servlet里面所关心的话题只有一个：返回的跳转路径
				ActionObjectUtil actionObjectUtil = new ActionObjectUtil(actionResult,request,this.actionClassMap) ;
				//
				dispatcherPath = actionObjectUtil.handleAction() ;	// 处理Action的反射操作
			} catch (Exception e) {
				e.printStackTrace();
			}
			ServletObjectUtil.clear() ;
		
		if (dispatcherPath != null) {	// 只能够判断null，而不能够判断有效性
			if (!"nopath".equals(dispatcherPath)) {
				request.getRequestDispatcher(dispatcherPath).forward(request, response);
			}
		} else {
			response.getWriter().println("ERROR REQUEST");
		}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		this.doGet(request, response);
	}
}
