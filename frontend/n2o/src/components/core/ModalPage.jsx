import React from 'react';
import PropTypes from 'prop-types';
import { Modal, ModalHeader, ModalBody, ModalFooter } from 'reactstrap';
import { compose } from 'recompose';
import Page from './Page';
import { connect } from 'react-redux';
import cn from 'classnames';
import { createStructuredSelector } from 'reselect';
import {
  makePageDisabledByIdSelector,
  makePageLoadingByIdSelector,
  makePageTitleByIdSelector
} from '../../selectors/pages';
import Actions from '../actions/Actions';
import factoryResolver from '../../utils/factoryResolver';
import withActions from './withActions';
import ModalDialog from '../actions/ModalDialog/ModalDialog';
import CoverSpinner from '../snippets/Spinner/CoverSpinner';
import { makeShowPromptByName } from '../../selectors/modals';

/**
 * Компонент, отображающий модальное окно
 * @reactProps {string} pageId - id пейджа
 * @reactProps {string} name - имя модалки
 * @reactProps {boolean} visible - отображается модалка или нет
 * @reactProps {string} size - размер('sm' или 'lg')
 * @reactProps {string} title - заголовок в хэдере
 * @reactProps {boolean} closeButton - Есть кнопка закрытия или нет
 * @reactProps {object} actions - объект экшнов
 * @reactProps {array} toolbar - массив, описывающий внений вид кнопок-экшенов
 * @reactProps {object} props - аргументы для экшенов-функций
 * @reactProps {boolean}  disabled - блокировка модалки
 * @example
 *  <ModalPage props={props}
 *             actions={actions}
 *             name={name}
 *             pageId={pageId}
 *  />
 */
class ModalPage extends React.Component {
  renderFromSrc(src) {
    const Component = factoryResolver(src, null);
    return <Component />;
  }

  render() {
    const {
      pageUrl,
      pageId,
      src,
      pathMapping,
      queryMapping,
      size,
      actions,
      containerKey,
      toolbar,
      visible,
      title,
      loading,
      disabled,
      close,
      name,
      showPrompt
    } = this.props;

    const pageMapping = {
      pathMapping,
      queryMapping
    };

    const showSpinner = !visible || loading || typeof loading === 'undefined';
    const classes = cn({ 'd-none': loading });
    return (
      <div className={cn('modal-page-overlay')}>
        {showPrompt && (
          <ModalDialog
            closeButton={true}
            text={'test'}
            close={() => {}}
            visible={showPrompt}
            onConfirm={() => close(name, false)}
            onDeny={() => {}}
          />
        )}
        {showSpinner && <CoverSpinner mode="transparent" />}
        <Modal
          isOpen={visible}
          toggle={() => close(name, true)}
          size={size}
          backdrop={false}
          style={{
            zIndex: 10
          }}
        >
          <ModalHeader className={classes} toggle={() => close(name, true)}>
            {title}
          </ModalHeader>
          <ModalBody className={classes}>
            {pageUrl ? (
              <Page pageUrl={pageUrl} pageId={pageId} pageMapping={pageMapping} />
            ) : src ? (
              this.renderFromSrc(src)
            ) : null}
          </ModalBody>
          {toolbar && (
            <ModalFooter className={classes}>
              <div className={cn('n2o-modal-actions', { 'n2o-disabled': disabled })}>
                <Actions
                  toolbar={toolbar.bottomLeft}
                  actions={actions}
                  containerKey={containerKey}
                  pageId={pageId}
                />
                <Actions
                  toolbar={toolbar.bottomRight}
                  actions={actions}
                  containerKey={containerKey}
                  pageId={pageId}
                />
              </div>
            </ModalFooter>
          )}
        </Modal>
      </div>
    );
  }
}

export const ModalWindow = ModalPage;

ModalPage.propTypes = {
  pageId: PropTypes.string,
  visible: PropTypes.bool,
  size: PropTypes.oneOf(['lg', 'sm']),
  title: PropTypes.string,
  closeButton: PropTypes.bool,
  toolbar: PropTypes.array,
  name: PropTypes.string,
  actions: PropTypes.object,
  props: PropTypes.object,
  close: PropTypes.func.isRequired,
  disabled: PropTypes.bool
};

ModalPage.defaultProps = {
  size: 'lg',
  disabled: false
};

const mapStateToProps = createStructuredSelector({
  title: (state, { pageId }) => makePageTitleByIdSelector(pageId)(state),
  loading: (state, { pageId }) => makePageLoadingByIdSelector(pageId)(state),
  disabled: (state, { pageId }) => makePageDisabledByIdSelector(pageId)(state),
  showPrompt: (state, { name }) => makeShowPromptByName(name)(state)
});

export default compose(
  connect(mapStateToProps),
  withActions
)(ModalPage);
