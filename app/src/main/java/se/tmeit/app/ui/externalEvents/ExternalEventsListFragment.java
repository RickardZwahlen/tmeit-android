package se.tmeit.app.ui.externalEvents;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import java.util.List;

import se.tmeit.app.R;
import se.tmeit.app.model.ExternalEvent;
import se.tmeit.app.services.Repository;
import se.tmeit.app.ui.ListFragmentBase;
import se.tmeit.app.ui.MainActivity;

/**
 * Fragment for the list of external events.
 */
public final class ExternalEventsListFragment extends ListFragmentBase {
    private static final String STATE_LIST_VIEW = "extEventsListState";
    private static final String TAG = ExternalEventsListFragment.class.getSimpleName();
    private final RepositoryResultHandler mRepositoryResultHandler = new RepositoryResultHandler();
    private List<ExternalEvent> mEvents;
    private ExternalEventsListAdapter mListAdapter;
    
    @Override
    public int getTitle() {
        return R.string.event_external_nav_title;
    }

    @Override
    protected void getDataFromRepository(Repository repository) {
        repository.getExternalEvents(mRepositoryResultHandler);
    }

    @Override
    protected String getStateKey() {
        return STATE_LIST_VIEW;
    }

    @Override
    protected void initializeList() {
        mListAdapter = new ExternalEventsListAdapter(getActivity(), mEvents);
        finishInitializeList(mListAdapter);
    }

    private final class RepositoryResultHandler implements Repository.RepositoryResultHandler<List<ExternalEvent>> {
        @Override
        public void onError(int errorMessage) {
            mEvents = null;
            onRepositoryError(errorMessage);
        }

        @Override
        public void onSuccess(List<ExternalEvent> result) {
            mEvents = result;
            onRepositorySuccess();
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (position < mListAdapter.getCount()) {
            ExternalEvent externalEvent = (ExternalEvent) mListAdapter.getItem(position);
            Fragment eventInfoFragment = ExternalEventInfoFragment.createInstance(getActivity(), externalEvent);
            Activity activity = getActivity();
            if (activity instanceof MainActivity) {
                saveInstanceState();
                MainActivity mainActivity = (MainActivity) activity;
                mainActivity.openFragment(eventInfoFragment, true);
            } else {
                Log.e(TAG, "Activity holding fragment is not MainActivity!");
            }
        }
    }
}
