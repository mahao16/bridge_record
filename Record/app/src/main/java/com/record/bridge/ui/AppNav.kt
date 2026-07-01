package com.record.bridge.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.record.bridge.LocalApp
import com.record.bridge.vm.AddRecordViewModel
import com.record.bridge.vm.AddRecordViewModelFactory
import com.record.bridge.vm.ProjectsViewModel
import com.record.bridge.vm.ProjectsViewModelFactory
import com.record.bridge.vm.ExportViewModel
import com.record.bridge.vm.ExportViewModelFactory
import com.record.bridge.vm.DictionarySettingsViewModel
import com.record.bridge.vm.DictionarySettingsViewModelFactory
import com.record.bridge.vm.RecordsViewModel
import com.record.bridge.vm.RecordsViewModelFactory
import com.record.bridge.vm.ProjectDetailViewModel
import com.record.bridge.vm.ProjectDetailViewModelFactory
import com.record.bridge.vm.SiteLogViewModel
import com.record.bridge.vm.SiteLogViewModelFactory

@Composable
fun AppNav(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val db = LocalApp.current.db

    NavHost(
        navController = navController,
        startDestination = "projects",
        modifier = modifier
    ) {
        composable("projects") {
            val vm: ProjectsViewModel =
                viewModel(factory = ProjectsViewModelFactory(db.projectDao(), db.projectRecordDao(), db.recordDao()))
            ProjectsScreen(
                vm = vm,
                onProjectClick = { p -> navController.navigate("records/${p.id}") },
                onSettingsClick = { navController.navigate("settings/dictionary") }
            )
        }
        composable("settings/dictionary") {
            val vm: DictionarySettingsViewModel = viewModel(factory = DictionarySettingsViewModelFactory(db.dictionaryDao()))
            DictionarySettingsScreen(
                vm = vm,
                onBack = { navController.popBackStack() }
            )
        }
        composable("records/{projectId}") { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId")?.toLongOrNull() ?: 0L
            val recordsVm: RecordsViewModel = viewModel(factory = RecordsViewModelFactory(projectId, db.recordDao()))
            val siteLogVm: SiteLogViewModel = viewModel(
                factory = SiteLogViewModelFactory(
                    siteLogDao = db.siteLogDao(),
                    dictionaryDao = db.dictionaryDao(),
                    projectId = projectId
                )
            )
            val detailVm: ProjectDetailViewModel = viewModel(
                factory = ProjectDetailViewModelFactory(
                    projectId = projectId,
                    projectDao = db.projectDao(),
                    recordDao = db.recordDao(),
                    siteLogDao = db.siteLogDao()
                )
            )
            val projectName by detailVm.projectName.collectAsState()
            ProjectDetailScreen(
                projectName = projectName,
                detailVm = detailVm,
                recordsVm = recordsVm,
                siteLogVm = siteLogVm,
                onBack = { navController.popBackStack() },
                onAddDisease = { navController.navigate("add/$projectId") },
                onRecordClick = { r ->
                    val c = Uri.encode(r.componentNo)
                    val t = Uri.encode(r.defectType)
                    val l = Uri.encode(r.defectLocation)
                    navController.navigate("edit/$projectId/$c/$t/$l")
                }
            )
        }
        composable("export/{projectId}") { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId")?.toLongOrNull() ?: 0L
            val vm: ExportViewModel = viewModel(
                factory = ExportViewModelFactory(
                    projectId = projectId,
                    projectDao = db.projectDao(),
                    recordDao = db.recordDao()
                )
            )
            ExportScreen(
                vm = vm,
                onBack = { navController.popBackStack() }
            )
        }
        composable("add/{projectId}") { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId")?.toLongOrNull() ?: 0L
            val vm: AddRecordViewModel = viewModel(
                factory = AddRecordViewModelFactory(
                    recordDao = db.recordDao(),
                    dictionaryDao = db.dictionaryDao(),
                    projectRecordDao = db.projectRecordDao(),
                    projectId = projectId
                )
            )
            AddRecordScreen(
                vm = vm,
                onBack = { navController.popBackStack() }
            )
        }
        composable("edit/{projectId}/{componentNo}/{defectType}/{defectLocation}") { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId")?.toLongOrNull() ?: 0L
            val componentNo = Uri.decode(backStackEntry.arguments?.getString("componentNo") ?: "")
            val defectType = Uri.decode(backStackEntry.arguments?.getString("defectType") ?: "")
            val defectLocation = Uri.decode(backStackEntry.arguments?.getString("defectLocation") ?: "")
            val vm: AddRecordViewModel = viewModel(
                factory = AddRecordViewModelFactory(
                    recordDao = db.recordDao(),
                    dictionaryDao = db.dictionaryDao(),
                    projectRecordDao = db.projectRecordDao(),
                    projectId = projectId
                )
            )
            EditRecordScreen(
                vm = vm,
                componentNo = componentNo,
                defectType = defectType,
                defectLocation = defectLocation,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

