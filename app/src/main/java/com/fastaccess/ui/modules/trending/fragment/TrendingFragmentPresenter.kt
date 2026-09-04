package com.fastaccess.ui.modules.trending.fragment

import android.view.View
import com.fastaccess.data.dao.TrendingModel
import com.fastaccess.helper.RxHelper
import com.fastaccess.provider.rest.jsoup.JsoupProvider
import com.fastaccess.ui.base.mvp.presenter.BasePresenter
import com.fastaccess.ui.modules.repos.RepoPagerActivity
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.util.*

/**
 * Created by Kosh on 30 May 2017, 11:04 PM
 */

class TrendingFragmentPresenter : BasePresenter<TrendingFragmentMvp.View>(),
    TrendingFragmentMvp.Presenter {

    private var disposel: Disposable? = null

    private val trendingList: ArrayList<TrendingModel> = ArrayList()

    override fun getTendingList(): ArrayList<TrendingModel> {
        return trendingList
    }

    override fun onItemLongClick(position: Int, v: View?, item: TrendingModel) {}

    override fun onItemClick(position: Int, v: View?, item: TrendingModel) {
        val context = v?.context ?: return
        // "owner / repo" as scraped. If the markup changed and the title came back
        // empty, skip the tap rather than crashing on split[1].
        val split = item.title?.split("/")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: return
        if (split.size < 2) return
        context.startActivity(
            RepoPagerActivity.createIntent(context, split[1], split[0])
        )
    }

    override fun onCallApi(lang: String, since: String) {
        disposel?.let { if (!it.isDisposed) it.dispose() }
        callApi(lang, since)
    }

    private fun callApi(
        lang: String,
        since: String
    ) {
        val language =
            if (lang == TrendingModel.DEFAULT_LANG) "" else lang.replace(" ", "_")
                .lowercase(Locale.getDefault())
        disposel = RxHelper.getObservable(
            JsoupProvider.getTrendingService(TrendingModel.PATH_URL).getTrending(language, since)
        )
            .doOnSubscribe {
                sendToView {
                    it.showProgress(0)
                    it.clearAdapter()
                }
            }.flatMap {
                RxHelper.getObservable(getTrendingObservable(it.body() ?: ""))
            }.subscribe(
                { response -> sendToView { view -> view.onNotifyAdapter(response) } },
                { throwable -> onError(throwable) },
                { sendToView { it.hideProgress() } }
            )
        manageDisposable(disposel)
    }


    private fun getTrendingObservable(response: String): Observable<List<TrendingModel>> {
        return Observable.fromPublisher { s ->
            val document: Document = Jsoup.parse(response, "")
            val repoList = document.select(".Box")
            val trendingList = arrayListOf<TrendingModel>()
            if (repoList.isNotEmpty()) {
                val list: Elements? = repoList.select(".Box-row")
                list?.let { it ->
                    if (list.isNotEmpty()) {
                        val models = it.map { element ->
                            // GitHub serves the repo name in <h2 class="h3 lh-condensed">;
                            // it used to be <h1>. Accept either so a future flip back
                            // does not silently blank every row again.
                            val title = element.select("h1 > a, h2 > a").text()
                            val description = element.select("p").text()
                            val stars = element.select(".f6 > a[href*=/stargazers]").text()
                            val forks = element.select(".f6 > a[href*=/network]").text()
                            var todayStars = element.select(".f6 > span.float-right").text()
                            if (todayStars.isNullOrBlank()) {
                                todayStars = element.select(".f6 > span.float-sm-right").text()
                            }
                            var language =
                                element.select(".f6 .mr-3 > span[itemprop=programmingLanguage]")
                                    .text()
                            if (language.isNullOrBlank()) {
                                language =
                                    element.select(".f6 span[itemprop=programmingLanguage]").text()
                            }
                            TrendingModel(
                                title,
                                description,
                                language,
                                stars,
                                forks,
                                todayStars
                            )
                        }
                        trendingList.addAll(models)
                    }
                }
            }
            s.onNext(trendingList)
            s.onComplete()
        }
    }
}