package net.jetztgrad.grails.spy

import org.codehaus.groovy.grails.commons.GrailsApplication

import org.springframework.beans.PropertyAccessorFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.support.AbstractApplicationContext

class SpyController {
	
	GrailsApplication grailsApplication

	static defaultAction = "inspect"

	def inspect = { 
		def beans = rootBeans
		
		// get bean to be inspected
		def path = params?.path?.toString()
		def parentPath = ''
		def inspected = null
		def inspectedName = ''
		
		if (!path) {
			// path is not available, try id
			path = params?.id
		}
		
		if (!path) {
			// no path, inspect the GrailsApplication object
			path = 'GrailsApplication'
		}
		
		if (path) {
			inspected = getBeanByPath(beans, path)
			List parts = path?.tokenize('/')
			inspectedName = parts?.last()
			if (path.contains('/')) {
				parentPath = path.substring(0, path.lastIndexOf('/'))
			}
		}
		
		//println "path: $path"
		//println "parentPath: $parentPath"
		//println "inspected: $inspectedName: $inspected"
		
		[ 
		 parentPath: parentPath,
		 path: path,
		 rootBeans: beans, 
		 inspected: inspected, 
		 inspectedName: inspectedName
		]
	}
	
	protected def getRootBeans() {
		[
		 	GrailsApplication: grailsApplication, 
		 	MainApplicationContext: grailsApplication?.mainContext,
			ParentApplicationContext: grailsApplication?.mainContext?.parent,
		]
	}
	
	protected def getBeanByPath(Map beans, def path) {
		if (!path) {
			return null
		}
		def parts = path.toString().tokenize('/')
		if (!parts) {
			return null
		}
		// get first bean, which is the root bean
		def rootName = parts.remove(0)
		def rootBean = beans.get(rootName)
		if (!rootBean) {
			return null
		}
		if (!parts) {
			return rootBean
		}
		def bean = rootBean
		for (name in parts) {
			def nextBean = null
			if (bean instanceof AbstractApplicationContext) {
				AbstractApplicationContext context = (AbstractApplicationContext) bean
				// lookup bean definition
				nextBean = getBeanDefinition(context, name.toString())

				if (!nextBean) {
					if (context.containsBean(name)) {
						nextBean = context.getBean(name)
					}
				}
			}
			
			if (!nextBean) {
				try {
					if (bean instanceof GroovyObject) {
						// access groovy style
						nextBean = bean.getProperty(name)
					}
					else {
						// access with accessor provided by spring
						nextBean = PropertyAccessorFactory.forBeanPropertyAccess(bean).getPropertyValue(name)
					}
				}
				catch (Exception e) {
					// ignore
				}
			}
			
			if (nextBean != null) {
				bean = nextBean
			}
			else {
				break
			}
		}
		
		return bean
	}
	
	protected def getBeanDefinition(AbstractApplicationContext context, String name) {
		// lookup bean definition
		if (context.beanFactory.containsBeanDefinition(name)) {
			return context.beanFactory.getBeanDefinition(name)
		}
		
		context = context.parent
		if (!context) {
			return null
		}
		
		return getBeanDefinition(context, name)
	}
}
