<#include "mcitems.ftl">
<#include "procedures.java.ftl">
<#assign customVals = {}>
<#list depInputs as depInput>
	<#if depInput.type() == "blockstate">
		<#assign customVals += {depInput.name(): mappedBlockToBlockStateCode(depInput.arg())}>
	<#elseif depInput.type() == "itemstack">
		<#assign customVals += {depInput.name(): mappedMCItemToItemStackCode(depInput.arg())}>
	<#else>
		<#assign customVals += {depInput.name(): depInput.arg()}>
	</#if>
</#list>
<@procedureToCode name=procedure dependencies=dependencies customVals=customVals />